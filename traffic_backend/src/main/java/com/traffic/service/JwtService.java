package com.traffic.service;

import com.traffic.common.UserStatus;
import com.traffic.entity.TaiKhoan;
import com.traffic.repository.TaiKhoanRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JwtService extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    // CÁC HÀM TIỆN ÍCH DÙNG CHO ĐĂNG NHẬP
    public String generateToken(TaiKhoan taiKhoan) {
        return buildToken(new HashMap<>(), taiKhoan, jwtExpiration);
    }

    public String generateRefreshToken(TaiKhoan taiKhoan) {
        return buildToken(new HashMap<>(), taiKhoan, refreshExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, TaiKhoan taiKhoan, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(taiKhoan.getTenDangNhap())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // BỘ LỌC CHẶN REQUEST THỜI GIAN THỰC
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            username = Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<TaiKhoan> taiKhoanOpt = taiKhoanRepository.findByTenDangNhap(username);

            if (taiKhoanOpt.isPresent()) {
                TaiKhoan tk = taiKhoanOpt.get();

                // Xác định quyền Admin an toàn không lo Lazy Loading lỗi
                boolean isAdmin = "admin".equalsIgnoreCase(tk.getTenDangNhap());

                if (!isAdmin) {
                    try {
                        List<String> roles = tk.getDanhSachPhanQuyen().stream()
                                .map(pq -> pq.getVaiTro().getTenVaiTro())
                                .collect(Collectors.toList());
                        isAdmin = roles.contains("ROLE_ADMIN");
                    } catch (Exception e) {
                        isAdmin = false;
                    }
                }

                // CHẶN NGƯỜI DÙNG THƯỜNG (Nếu bị khóa hoặc điểm thấp)
                if (!isAdmin) {
                    boolean isLockedByStatus = UserStatus.LOCKED.equals(tk.getTrangThai()) || "LOCKED".equals(String.valueOf(tk.getTrangThai()));
                    boolean isLockedByPoint = (tk.getDoTinCayNguoiDung() != null && tk.getDoTinCayNguoiDung() < 5);

                    if (isLockedByStatus || isLockedByPoint) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"status\": 403, \"message\": \"Tài khoản của bạn đã bị khóa!\"}");
                        return;
                    }
                }

                // KIỂM TRA THỜI HẠN TOKEN
                boolean isExpired = false;
                try {
                    Date expiration = Jwts.parserBuilder()
                            .setSigningKey(getSignInKey())
                            .build()
                            .parseClaimsJws(jwt)
                            .getBody()
                            .getExpiration();
                    isExpired = expiration.before(new Date());
                } catch (Exception e) {
                    isExpired = true;
                }

                if (!isExpired) {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                    // Nạp quyền an toàn cho hệ thống
                    if (isAdmin) {
                        // Nếu là tài khoản Admin, gán thẳng quyền tránh lỗi Lazy Loading từ Database
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    } else {
                        try {
                            authorities = tk.getDanhSachPhanQuyen().stream()
                                    .map(pq -> new SimpleGrantedAuthority(pq.getVaiTro().getTenVaiTro()))
                                    .collect(Collectors.toList());
                        } catch (Exception e) {
                            // Mặc định quyền USER nếu lỗi nạp danh sách
                            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                        }
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            tk, null, authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    // CÁC HÀM PHỤC VỤ REFRESH TOKEN
    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
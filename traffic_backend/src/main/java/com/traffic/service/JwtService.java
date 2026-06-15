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

                // 1. KIỂM TRA QUYỀN ADMIN (Bằng tên đăng nhập hoặc quét danh sách quyền)
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

                // 2. CHẶN NGƯỜI DÙNG THƯỜNG (Sử dụng chính xác Enum UserStatus bạn gửi)
                if (!isAdmin) {
                    boolean isLockedByStatus = false;

                    if (tk.getTrangThai() != null) {
                        // Kiểm tra nếu trạng thái bằng đúng Enum LOCKED hoặc chuỗi "LOCKED"
                        isLockedByStatus = UserStatus.LOCKED.equals(tk.getTrangThai())
                                || "LOCKED".equalsIgnoreCase(String.valueOf(tk.getTrangThai()));
                    }

                    // Kiểm tra điểm tin cậy (Null-safe)
                    int diemTinCay = (tk.getDoTinCayNguoiDung() != null) ? tk.getDoTinCayNguoiDung() : 50;
                    boolean isLockedByPoint = (diemTinCay < 5);

                    // Nếu dính 1 trong 2 điều kiện thì chặn luôn
                    if (isLockedByStatus || isLockedByPoint) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"status\": 403, \"message\": \"Tài khoản của bạn đã bị khóa hoặc hạ điểm uy tín do vi phạm!\"}");
                        return;
                    }
                }

                // 3. KIỂM TRA THỜI HẠN TOKEN
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

                    if (isAdmin) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    } else {
                        try {
                            authorities = tk.getDanhSachPhanQuyen().stream()
                                    .map(pq -> new SimpleGrantedAuthority(pq.getVaiTro().getTenVaiTro()))
                                    .collect(Collectors.toList());

                            if (authorities.isEmpty()) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                            }
                        } catch (Exception e) {
                            // Cứu cánh khi bị lỗi Lazy Loading Session
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
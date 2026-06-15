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

    // CÁC HÀM TIỆN ÍCH DÙNG CHO ĐĂNG NHẬP (AuthServiceImpl gọi sang)
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

    // BỘ LỌC CHẶN REQUEST THỜI GIAN THỰC (Bảo mật Spring Security)
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Nếu không có header token hợp lệ thì cho đi qua để Spring Security xử lý phân quyền sau
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        // Tiến hành giải mã Token lấy tên đăng nhập
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

        // Kiểm tra tài khoản và chặn trạng thái Khóa Real-time
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<TaiKhoan> taiKhoanOpt = taiKhoanRepository.findByTenDangNhap(username);

            if (taiKhoanOpt.isPresent()) {
                TaiKhoan tk = taiKhoanOpt.get();

                List<String> roles = tk.getDanhSachPhanQuyen().stream()
                        .map(pq -> pq.getVaiTro().getTenVaiTro())
                        .collect(Collectors.toList());
                boolean isAdmin = roles.contains("ROLE_ADMIN");

                //  LOGIC ĐẨY TÀI KHOẢN KHI ADMIN KHÓA
                boolean isLockedByStatus = UserStatus.LOCKED.equals(tk.getTrangThai()) || "LOCKED".equals(String.valueOf(tk.getTrangThai()));
                boolean isLockedByPoint = (tk.getDoTinCayNguoiDung() != null && tk.getDoTinCayNguoiDung() < 5);

                if (!isAdmin && (isLockedByStatus || isLockedByPoint)) {
                    // Trả về mã lỗi 403 để kích hoạt Force Logout ở file axiosConfig.js phía React
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\": 403, \"message\": \"Tài khoản của bạn đã bị khóa!\"}");
                    return;
                }

                // Kiểm tra thời hạn Token
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

                // Nếu token hợp lệ, nạp thông tin đăng nhập vào Context hệ thống
                if (!isExpired) {
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            tk, null, authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    // REFRESH TOKEN TRONG AUTH_SERVICE
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
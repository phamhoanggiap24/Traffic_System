package com.traffic.service;

import com.traffic.common.RoleConstant;
import com.traffic.common.UserStatus;
import com.traffic.entity.TaiKhoan;
import com.traffic.repository.TaiKhoanRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
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

    public String generateToken(TaiKhoan taiKhoan) {
        Map<String, Object> extraClaims = new HashMap<>();
        try {
            if (taiKhoan.getDanhSachPhanQuyen() != null) {
                List<String> roles = taiKhoan.getDanhSachPhanQuyen().stream()
                        .filter(pq -> pq.getVaiTro() != null)
                        .map(pq -> pq.getVaiTro().getTenVaiTro())
                        .collect(Collectors.toList());
                extraClaims.put("roles", roles);
            }
        } catch (Exception e) {
            // Tránh Lazy Loading lỗi lúc đăng ký
        }
        return buildToken(extraClaims, taiKhoan, jwtExpiration);
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        String username = extractUsername(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Sử dụng hàm JOIN FETCH bạn đã viết rất chuẩn ở Repo
            Optional<TaiKhoan> taiKhoanOpt = taiKhoanRepository.findProfileByTenDangNhap(username);

            if (taiKhoanOpt.isPresent()) {
                TaiKhoan tk = taiKhoanOpt.get();

                // 1. Kiểm tra tài khoản đặc quyền admin tối cao
                boolean isAdmin = "admin".equalsIgnoreCase(tk.getTenDangNhap()) ||
                        (tk.getDanhSachPhanQuyen() != null && tk.getDanhSachPhanQuyen().stream()
                                .anyMatch(pq -> pq.getVaiTro() != null &&
                                        (RoleConstant.ROLE_ADMIN.equalsIgnoreCase(pq.getVaiTro().getTenVaiTro()) ||
                                                "ADMIN".equalsIgnoreCase(pq.getVaiTro().getTenVaiTro()))));

                // 2. Kiểm tra trạng thái khóa (Bỏ qua điều kiện chặn nếu là Admin)
                if (!isAdmin) {
                    boolean isLocked = UserStatus.LOCKED.equals(tk.getTrangThai()) ||
                            (tk.getDoTinCayNguoiDung() != null && tk.getDoTinCayNguoiDung() < 5);
                    if (isLocked) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"status\": 403, \"message\": \"Tài khoản bị khóa!\"}");
                        return;
                    }
                }

                // 3. Cơ chế tạo danh sách quyền bất tử - Nạp mọi định dạng để không bao giờ bị lệch config
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                if (tk.getDanhSachPhanQuyen() != null) {
                    for (var pq : tk.getDanhSachPhanQuyen()) {
                        if (pq.getVaiTro() != null && pq.getVaiTro().getTenVaiTro() != null) {
                            String role = pq.getVaiTro().getTenVaiTro().trim().toUpperCase();

                            // Nạp chuỗi gốc từ DB
                            authorities.add(new SimpleGrantedAuthority(role));
                            authorities.add(new SimpleGrantedAuthority(pq.getVaiTro().getTenVaiTro().trim()));

                            // Nếu DB không chứa tiền tố ROLE_, sinh thêm một bản quyền dạng ROLE_ để đáp ứng SecurityConfig
                            if (!role.startsWith("ROLE_")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                            }
                        }
                    }
                }

                // Nếu xác định tài khoản là Admin, ép chết cả hai dạng quyền Admin cao nhất vào hệ thống
                if (isAdmin) {
                    authorities.add(new SimpleGrantedAuthority("ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }

                if (authorities.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("USER"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        tk, null, authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

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
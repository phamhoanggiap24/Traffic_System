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

    // HÀM TẠO ACCESSTOKEN: Nạp danh sách quyền (Roles) vào Claims của JWT
    public String generateToken(TaiKhoan taiKhoan) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (taiKhoan.getDanhSachPhanQuyen() != null) {
            List<String> roles = taiKhoan.getDanhSachPhanQuyen().stream()
                    .filter(pq -> pq.getVaiTro() != null)
                    .map(pq -> pq.getVaiTro().getTenVaiTro())
                    .collect(Collectors.toList());
            extraClaims.put("roles", roles); // Đẩy mảng các quyền vào Token
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

    // BỘ LỌC KIỂM TRA VÀ DUYỆT QUYỀN MỖI REQUEST
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        String username = extractUsername(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Nạp dữ liệu tài khoản bằng hàm FETCH JOIN tối ưu từ Repo của bạn
            Optional<TaiKhoan> taiKhoanOpt = taiKhoanRepository.findProfileByTenDangNhap(username);

            if (taiKhoanOpt.isPresent()) {
                TaiKhoan tk = taiKhoanOpt.get();

                // 1. Kiểm tra Admin đặc cách (Dựa trên username hoặc bảng phân quyền DB)
                boolean isAdmin = "admin".equalsIgnoreCase(tk.getTenDangNhap()) ||
                        (tk.getDanhSachPhanQuyen() != null && tk.getDanhSachPhanQuyen().stream()
                                .anyMatch(pq -> pq.getVaiTro() != null &&
                                        (RoleConstant.ROLE_ADMIN.equalsIgnoreCase(pq.getVaiTro().getTenVaiTro()) ||
                                                "ADMIN".equalsIgnoreCase(pq.getVaiTro().getTenVaiTro()))));

                // 2. Kiểm tra trạng thái khóa (Nếu là đặc quyền Admin thì bỏ qua bước chặn)
                if (!isAdmin) {
                    boolean isLocked = UserStatus.LOCKED.equals(tk.getTrangThai()) ||
                            (tk.getDoTinCayNguoiDung() != null && tk.getDoTinCayNguoiDung() < 5);
                    if (isLocked) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"status\": 403, \"message\": \"Tài khoản đã bị khóa!\"}");
                        return;
                    }
                }

                // 3. Đọc danh sách quyền trực tiếp từ Token Payload (Claims)
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                try {
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(getSignInKey())
                            .build()
                            .parseClaimsJws(jwt)
                            .getBody();

                    List<?> roles = claims.get("roles", List.class);
                    if (roles != null) {
                        for (Object roleObj : roles) {
                            String roleStr = String.valueOf(roleObj).trim();
                            authorities.add(new SimpleGrantedAuthority(roleStr));
                            // Hỗ trợ phòng hờ cả dạng không có tiền tố ROLE_
                            if (!roleStr.toUpperCase().startsWith("ROLE_")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleStr.toUpperCase()));
                            }
                        }
                    }
                } catch (Exception e) {
                    // Nếu lỗi Token không đọc được claims, fallback đọc từ dữ liệu DB vừa nạp
                    if (tk.getDanhSachPhanQuyen() != null) {
                        tk.getDanhSachPhanQuyen().forEach(pq -> {
                            if (pq.getVaiTro() != null) {
                                String roleStr = pq.getVaiTro().getTenVaiTro();
                                authorities.add(new SimpleGrantedAuthority(roleStr));
                                if (!roleStr.toUpperCase().startsWith("ROLE_")) {
                                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleStr.toUpperCase()));
                                }
                            }
                        });
                    }
                }

                // Nếu hệ thống xác định tài khoản là Admin, ép chặt quyền ADMIN vào context
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
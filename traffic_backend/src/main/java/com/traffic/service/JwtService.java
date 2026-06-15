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
            // SỬ DỤNG HÀM FETCH JOIN MẠNH NHẤT TỪ REPO CỦA BẠN ĐỂ LẤY FULL QUYỀN TỪ DB
            Optional<TaiKhoan> taiKhoanOpt = taiKhoanRepository.findProfileByTenDangNhap(username);

            if (taiKhoanOpt.isPresent()) {
                TaiKhoan tk = taiKhoanOpt.get();

                // 1. Kiểm tra quyền Admin (Nếu tên đăng nhập là "admin" thì auto là Admin tối cao)
                boolean isAdmin = "admin".equalsIgnoreCase(tk.getTenDangNhap()) ||
                        (tk.getDanhSachPhanQuyen() != null && tk.getDanhSachPhanQuyen().stream()
                                .anyMatch(pq -> pq.getVaiTro() != null &&
                                        (RoleConstant.ROLE_ADMIN.equalsIgnoreCase(pq.getVaiTro().getTenVaiTro()) ||
                                                "ADMIN".equalsIgnoreCase(pq.getVaiTro().getTenVaiTro()))));

                // 2. Kiểm tra trạng thái khóa (Nếu là admin thì bỏ qua không chặn)
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

                // 3. ÉP CỨNG QUYỀN VÀO CONTEXT (BẤT CHẤP TOKEN CŨ HAY MỚI)
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                if (tk.getDanhSachPhanQuyen() != null) {
                    for (var pq : tk.getDanhSachPhanQuyen()) {
                        if (pq.getVaiTro() != null && pq.getVaiTro().getTenVaiTro() != null) {
                            String rawRole = pq.getVaiTro().getTenVaiTro().trim();

                            // Thêm quyền dạng thô từ DB (ví dụ: ROLE_ADMIN hoặc ADMIN)
                            authorities.add(new SimpleGrantedAuthority(rawRole));

                            // Nếu DB chưa có tiền tố ROLE_, nạp thêm một bản có chữ ROLE_ cho khớp với Config
                            if (!rawRole.toUpperCase().startsWith("ROLE_")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + rawRole.toUpperCase()));
                            }
                        }
                    }
                }

                // Nếu là tài khoản admin, ép thêm chắc chắn hai quyền cao nhất
                if (isAdmin) {
                    authorities.add(new SimpleGrantedAuthority("ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }

                // Nếu danh sách trống, mặc định gán quyền USER cơ bản
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
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
import java.util.function.Function;
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

    // HÀM TẠO TOKEN: Nạp sẵn danh sách các quyền vào Payload Claims
    public String generateToken(TaiKhoan taiKhoan) {
        Map<String, Object> claims = new HashMap<>();
        try {
            if (taiKhoan.getDanhSachPhanQuyen() != null) {
                List<String> roles = taiKhoan.getDanhSachPhanQuyen().stream()
                        .filter(pq -> pq.getVaiTro() != null)
                        .map(pq -> pq.getVaiTro().getTenVaiTro())
                        .collect(Collectors.toList());
                claims.put("roles", roles); // Lưu danh sách quyền vào Token
            }
        } catch (Exception e) {
            // Đề phòng LazyLoading lúc đăng ký tài khoản mới chưa fetch
        }
        return buildToken(claims, taiKhoan, jwtExpiration);
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

    // BỘ LỌC CHẶN REQUEST VÀ ĐỒNG BỘ QUYỀN AN TOÀN
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
            // Sử dụng hàm JOIN FETCH chuẩn đã viết trong Repo của bạn
            Optional<TaiKhoan> taiKhoanOpt = taiKhoanRepository.findProfileByTenDangNhap(username);

            if (taiKhoanOpt.isPresent()) {
                TaiKhoan tk = taiKhoanOpt.get();

                // 1. Kiểm tra đặc quyền Admin (Bằng tên đăng nhập hoặc bảng quyền DB)
                boolean isAdmin = "admin".equalsIgnoreCase(tk.getTenDangNhap()) ||
                        (tk.getDanhSachPhanQuyen() != null && tk.getDanhSachPhanQuyen().stream()
                                .anyMatch(pq -> pq.getVaiTro() != null &&
                                        (RoleConstant.ROLE_ADMIN.equalsIgnoreCase(pq.getVaiTro().getTenVaiTro()) ||
                                                "ADMIN".equalsIgnoreCase(pq.getVaiTro().getTenVaiTro()))));

                // 2. Kiểm tra trạng thái khóa (Bỏ qua nếu là Admin tối cao)
                if (!isAdmin) {
                    boolean isLocked = UserStatus.LOCKED.equals(tk.getTrangThai()) ||
                            (tk.getDoTinCayNguoiDung() != null && tk.getDoTinCayNguoiDung() < 5);
                    if (isLocked) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"status\": 403, \"message\": \"Tài khoản của bạn đã bị khóa!\"}");
                        return;
                    }
                }

                // 3. Chuẩn hóa nạp quyền (Authorities) cho Spring Security Context
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                if (tk.getDanhSachPhanQuyen() != null && !tk.getDanhSachPhanQuyen().isEmpty()) {
                    for (var pq : tk.getDanhSachPhanQuyen()) {
                        if (pq.getVaiTro() != null && pq.getVaiTro().getTenVaiTro() != null) {
                            String rawRole = pq.getVaiTro().getTenVaiTro().trim();
                            authorities.add(new SimpleGrantedAuthority(rawRole));

                            if (!rawRole.toUpperCase().startsWith("ROLE_")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + rawRole.toUpperCase()));
                            }
                        }
                    }
                }

                // Ép quyền Admin mặc định cho tài khoản admin cấp cao để loại bỏ hoàn toàn nguy cơ 403
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
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractAllClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
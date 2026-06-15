package com.traffic.service.impl;

import com.traffic.common.ApiResponse;
import com.traffic.common.AuthMessage;
import com.traffic.common.RoleConstant;
import com.traffic.common.UserStatus;
import com.traffic.dto.request.*;
import com.traffic.dto.response.AuthResponse;
import com.traffic.dto.response.ResetPasswordResponse;
import com.traffic.dto.response.UserManagementResponse;
import com.traffic.entity.*;
import com.traffic.repository.*;
import com.traffic.service.AuthService;
import com.traffic.service.EmailService;
import com.traffic.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired private TaiKhoanRepository taiKhoanRepository;
    @Autowired private JwtService jwtService;
    @Autowired private VaiTroRepository vaiTroRepository;
    @Autowired private PhanQuyenRepository phanQuyenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private QuenMatKhauRepository quenMatKhauRepository;
    @Autowired private ModelMapper modelMapper;
    @Autowired private NhatKyHeThongRepository nhatKyHeThongRepository;
    @Autowired private EmailService emailService;

    @Override
    @Transactional
    public ApiResponse<UserManagementResponse> register(RegisterRequest request) {
        if (taiKhoanRepository.existsByTenDangNhap(request.getTenDangNhap()))
            return ApiResponse.error(400, "Tên đăng nhập đã tồn tại!");
        if (taiKhoanRepository.existsByEmail(request.getEmail()))
            return ApiResponse.error(400, "Email đã tồn tại!");

        String token = UUID.randomUUID().toString();
        // Gửi mail bất đồng bộ (không làm treo ứng dụng)
        emailService.sendVerificationEmail(request.getEmail(), request.getHoTen(), token);

        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap(request.getTenDangNhap());
        tk.setMatKhau(passwordEncoder.encode(request.getMatKhau()));
        tk.setEmail(request.getEmail());
        tk.setHoTen(request.getHoTen());
        tk.setSoDienThoai(request.getSoDienThoai());
        tk.setVerificationToken(token);
        tk.setTokenCreatedAt(LocalDateTime.now());
        tk.setTrangThai(UserStatus.INACTIVE);
        tk.setDoTinCayNguoiDung(50);

        TaiKhoan savedTk = taiKhoanRepository.save(tk);

        VaiTro vaiTroUser = vaiTroRepository.findByTenVaiTro(RoleConstant.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy vai trò ROLE_USER."));

        PhanQuyen pq = new PhanQuyen();
        pq.setTaiKhoan(savedTk);
        pq.setVaiTro(vaiTroUser);
        phanQuyenRepository.save(pq);

        UserManagementResponse userDto = modelMapper.map(savedTk, UserManagementResponse.class);
        userDto.setVaiTro(List.of(vaiTroUser.getTenVaiTro()));
        userDto.setDoTinCayNguoiDung(savedTk.getDoTinCayNguoiDung());

        return ApiResponse.success("Đăng ký thành công! Vui lòng kiểm tra Email để kích hoạt.", userDto);
    }

    @Override
    public int verifyAccount(String token) {
        Optional<TaiKhoan> optionalTk = taiKhoanRepository.findByVerificationToken(token);
        if (optionalTk.isPresent()) {
            TaiKhoan tk = optionalTk.get();
            if (java.time.Duration.between(tk.getTokenCreatedAt(), LocalDateTime.now()).toHours() > 24) return 0;
            tk.setTrangThai(UserStatus.ACTIVE);
            tk.setVerificationToken(null);
            tk.setTokenCreatedAt(null);
            taiKhoanRepository.save(tk);
            return 1;
        }
        return -1;
    }

    @Override
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        Optional<TaiKhoan> taiKhoanOpt = taiKhoanRepository.findProfileByTenDangNhap(request.getTenDangNhap());
        if (taiKhoanOpt.isEmpty()) return ApiResponse.error(404, "Tài khoản không tồn tại!");

        TaiKhoan tk = taiKhoanOpt.get();
        if (tk.getTrangThai() == UserStatus.INACTIVE) return ApiResponse.error(403, "Tài khoản chưa kích hoạt!");

        List<String> roles = tk.getDanhSachPhanQuyen().stream().map(pq -> pq.getVaiTro().getTenVaiTro()).collect(Collectors.toList());
        boolean isAdmin = roles.contains(RoleConstant.ROLE_ADMIN);

        // 1. Tính toán trạng thái khóa an toàn bằng cách ép chuỗi ép kiểu
        boolean isLockedByStatus = tk.getTrangThai() == UserStatus.LOCKED || "LOCKED".equals(String.valueOf(tk.getTrangThai()));
        boolean isLockedByPoint = (tk.getDoTinCayNguoiDung() != null && tk.getDoTinCayNguoiDung() < 5);

        // 2. Kiểm tra điều kiện chặn đăng nhập
        if (!isAdmin && (isLockedByStatus || isLockedByPoint)) {
            saveSystemLog(tk, "LOGIN_BLOCKED", "Tài khoản bị khóa.");
            return ApiResponse.error(403, "Tài khoản bị khóa do vi phạm hoặc điểm tin cậy thấp!");
        }

        if (!passwordEncoder.matches(request.getMatKhau(), tk.getMatKhau()))
            return ApiResponse.error(401, "Mật khẩu không chính xác!");

        UserManagementResponse userDto = modelMapper.map(tk, UserManagementResponse.class);
        userDto.setVaiTro(roles);
        userDto.setDoTinCayNguoiDung(tk.getDoTinCayNguoiDung() != null ? tk.getDoTinCayNguoiDung() : 50);

        AuthResponse authData = AuthResponse.builder()
                .accessToken(jwtService.generateToken(tk))
                .refreshToken(jwtService.generateRefreshToken(tk))
                .user(userDto)
                .build();

        saveSystemLog(tk, "LOGIN_SUCCESS", "Đăng nhập thành công.");
        return ApiResponse.success("Đăng nhập thành công!", authData);
    }

    @Override
    public ApiResponse<ResetPasswordResponse> forgotPassword(String email) {
        TaiKhoan tk = taiKhoanRepository.findByEmail(email).orElse(null);
        if (tk == null) return ApiResponse.error(404, AuthMessage.EMAIL_NOT_FOUND.getMessage());

        String otp = String.valueOf((int)((Math.random() * 899999) + 100000));
        QuenMatKhau qmk = new QuenMatKhau();
        qmk.setEmail(email);
        qmk.setOtpCode(otp);
        qmk.setThoiGianHetHan(LocalDateTime.now().plusMinutes(5));
        qmk.setDaSuDung(false);
        qmk.setTaiKhoan(tk);
        quenMatKhauRepository.save(qmk);

        emailService.sendOtpPasswordEmail(email, otp); // Gửi OTP bất đồng bộ
        return ApiResponse.success(AuthMessage.OTP_SENT.getMessage(),
                ResetPasswordResponse.builder().email(email).thoiGianHetHan(qmk.getThoiGianHetHan()).build());
    }

    @Override
    @Transactional
    public ApiResponse<String> resetPassword(ResetPasswordRequest request) {
        QuenMatKhau qmk = quenMatKhauRepository.findFirstByEmailAndDaSuDungFalseOrderByThoiGianHetHanDesc(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Yêu cầu không hợp lệ!"));

        if (!qmk.getOtpCode().equals(request.getOtp()) || qmk.getThoiGianHetHan().isBefore(LocalDateTime.now()))
            return ApiResponse.error(400, "Mã OTP không đúng hoặc hết hạn!");

        TaiKhoan tk = qmk.getTaiKhoan();
        tk.setMatKhau(passwordEncoder.encode(request.getMatKhauMoi()));
        taiKhoanRepository.save(tk);
        qmk.setDaSuDung(true);
        quenMatKhauRepository.save(qmk);

        return ApiResponse.success("Đổi mật khẩu thành công!", null);
    }

    @Override
    @Transactional
    public ApiResponse<UserManagementResponse> createAdminAccount(RegisterRequest request) {
        if (taiKhoanRepository.existsByTenDangNhap(request.getTenDangNhap())) return ApiResponse.error(400, "Admin đã tồn tại!");
        TaiKhoan admin = new TaiKhoan();
        admin.setTenDangNhap(request.getTenDangNhap());
        admin.setMatKhau(passwordEncoder.encode(request.getMatKhau()));
        admin.setEmail(request.getEmail());
        admin.setHoTen(request.getHoTen());
        admin.setTrangThai(UserStatus.ACTIVE);
        admin.setDoTinCayNguoiDung(50);
        TaiKhoan saved = taiKhoanRepository.save(admin);

        VaiTro adminRole = vaiTroRepository.findByTenVaiTro(RoleConstant.ROLE_ADMIN).orElseThrow();
        PhanQuyen pq = new PhanQuyen();
        pq.setTaiKhoan(saved);
        pq.setVaiTro(adminRole);
        phanQuyenRepository.save(pq);

        UserManagementResponse res = modelMapper.map(saved, UserManagementResponse.class);
        res.setVaiTro(List.of(RoleConstant.ROLE_ADMIN));
        return ApiResponse.success("Tạo Admin thành công!", res);
    }

    @Override
    public ApiResponse<AuthResponse> changePassword(ChangePasswordRequest request) {
        TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(request.getTenDangNhap()).orElseThrow();
        if (!passwordEncoder.matches(request.getMatKhauCu(), tk.getMatKhau())) return ApiResponse.error(400, "Sai mật khẩu cũ!");
        tk.setMatKhau(passwordEncoder.encode(request.getMatKhauMoi()));
        taiKhoanRepository.save(tk);
        return ApiResponse.success("Đổi mật khẩu thành công!", null);
    }

    @Override
    public ApiResponse<AuthResponse> refreshToken(String refreshToken) {
        try {
            String username = jwtService.extractUsername(refreshToken);
            var user = taiKhoanRepository.findByTenDangNhap(username).orElseThrow();
            if (!jwtService.isTokenExpired(refreshToken)) {
                List<String> roles = user.getDanhSachPhanQuyen().stream().map(pq -> pq.getVaiTro().getTenVaiTro()).collect(Collectors.toList());
                UserManagementResponse userDto = modelMapper.map(user, UserManagementResponse.class);
                userDto.setVaiTro(roles);

                return ApiResponse.success("Làm mới Token thành công", AuthResponse.builder()
                        .accessToken(jwtService.generateToken(user)).refreshToken(refreshToken).user(userDto).build());
            }
        } catch (Exception e) { return ApiResponse.error(401, "Refresh Token không hợp lệ!"); }
        return ApiResponse.error(401, "Lỗi Refresh Token");
    }

    @Override
    public ApiResponse<Void> logout(String token) {
        return ApiResponse.success("Đăng xuất thành công", null);
    }

    private void saveSystemLog(TaiKhoan tk, String hanhDong, String moTa) {
        try {
            NhatKyHeThong log = new NhatKyHeThong();
            log.setTaiKhoan(tk); log.setHanhDong(hanhDong); log.setMoTa(moTa); log.setDiaChiIp("127.0.0.1");
            nhatKyHeThongRepository.save(log);
        } catch (Exception e) { System.err.println("Lỗi lưu log: " + e.getMessage()); }
    }
}
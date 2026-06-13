package com.traffic.service.impl;

import com.traffic.common.ApiResponse;
import com.traffic.common.AuthMessage;
import com.traffic.common.RoleConstant;
import com.traffic.common.UserStatus;
import com.traffic.dto.request.ChangePasswordRequest;
import com.traffic.dto.request.LoginRequest;
import com.traffic.dto.request.RegisterRequest;
import com.traffic.dto.request.ResetPasswordRequest;
import com.traffic.dto.response.AuthResponse;
import com.traffic.dto.response.ResetPasswordResponse;
import com.traffic.dto.response.UserManagementResponse;
import com.traffic.entity.*;
import com.traffic.repository.*;
import com.traffic.service.AuthService;
import com.traffic.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private VaiTroRepository vaiTroRepository;

    @Autowired
    private PhanQuyenRepository phanQuyenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private QuenMatKhauRepository quenMatKhauRepository;

    @Value("${spring.mail.username:${MAIL_USERNAME:your-email@gmail.com}}")
    private String emailSender;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private NhatKyHeThongRepository nhatKyHeThongRepository;

    @Override
    @Transactional
    public ApiResponse<UserManagementResponse> register(RegisterRequest request) {
        // 1. Kiểm tra tên đăng nhập và email đã tồn tại chưa
        if (taiKhoanRepository.existsByTenDangNhap(request.getTenDangNhap())) {
            return ApiResponse.error(400, "Tên đăng nhập đã tồn tại!");
        }

        if (taiKhoanRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.error(400, "Email đã tồn tại!");
        }

        // Tạo mã Token bí mật
        String token = UUID.randomUUID().toString();

        // Tạo link xác thực
        String verifyLink = "http://localhost:8080/api/auth/verify?token=" + token;

        // Thử gửi mail để xác thực email thật
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailSender);
            message.setTo(request.getEmail());
            message.setSubject("XÁC NHẬN ĐĂNG KÝ HỆ THỐNG GIAO THÔNG");
            message.setText("Chào " + request.getHoTen() + ",\n\n" +
                    "Cảm ơn bạn đã đăng ký. Vui lòng nhấn vào đường link bên dưới để kích hoạt tài khoản:\n" +
                    verifyLink + "\n\n" +
                    "Link này sẽ hết hạn sau 24 giờ.");

            mailSender.send(message);
        } catch (Exception e) {
            return ApiResponse.error(400, "Email không tồn tại hoặc không thể nhận tin nhắn!");
        }

        // Tạo thực thể TaiKhoan
        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap(request.getTenDangNhap());
        tk.setMatKhau(passwordEncoder.encode(request.getMatKhau()));
        tk.setEmail(request.getEmail());
        tk.setHoTen(request.getHoTen());
        tk.setSoDienThoai(request.getSoDienThoai());
        tk.setVerificationToken(token);
        tk.setTokenCreatedAt(LocalDateTime.now());
        tk.setTrangThai(UserStatus.INACTIVE);

        TaiKhoan savedTk = taiKhoanRepository.save(tk);

        // Gán vai trò mặc định (ROLE_USER)
        VaiTro vaiTroUser = vaiTroRepository.findByTenVaiTro(RoleConstant.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy vai trò ROLE_USER."));

        PhanQuyen pq = new PhanQuyen();
        pq.setTaiKhoan(savedTk);
        pq.setVaiTro(vaiTroUser);

        phanQuyenRepository.save(pq);

        // Trả về UserResponse
        UserManagementResponse userDto = modelMapper.map(savedTk, UserManagementResponse.class);
        userDto.setVaiTro(List.of(vaiTroUser.getTenVaiTro()));

        return ApiResponse.success("Đăng ký thành công! Vui lòng kiểm tra Email để kích hoạt tài khoản.", userDto);
    }

    @Override
    public int verifyAccount(String token) {
        // Tìm tài khoản nào đang giữ mã token này
        Optional<TaiKhoan> optionalTk = taiKhoanRepository.findByVerificationToken(token);

        if (optionalTk.isPresent()) {
            TaiKhoan tk = optionalTk.get();

            // Kiểm tra hết hạn
            LocalDateTime now = LocalDateTime.now();
            long hours = java.time.Duration.between(tk.getTokenCreatedAt(), now).toHours();

            if (hours > 24) {
                return 0;
            }

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
        // Tìm tài khoản theo username
        Optional<TaiKhoan> taiKhoanOpt = taiKhoanRepository.findByTenDangNhap(request.getTenDangNhap());

        if (taiKhoanOpt.isEmpty()) {
            return ApiResponse.<AuthResponse>error(404, "Tài khoản không tồn tại!");
        }

        TaiKhoan tk = taiKhoanOpt.get();

        // Chặn nếu tài khoản chưa kích hoạt qua Gmail
        if (tk.getTrangThai() == UserStatus.INACTIVE) {
            return ApiResponse.<AuthResponse>error(403, "Tài khoản chưa được kích hoạt. Vui lòng kiểm tra Gmail để xác nhận!");
        }

        // KIỂM TRA PHÂN QUYỀN
        List<String> roles = tk.getDanhSachPhanQuyen().stream()
                .map(pq -> pq.getVaiTro().getTenVaiTro())
                .collect(Collectors.toList());
        boolean isAdmin = roles.contains(RoleConstant.ROLE_ADMIN);

        // CHỈ KHÓA NẾU KHÔNG PHẢI LÀ ADMIN
        boolean isLockedByPoint = (tk.getDoTinCayNguoiDung() != null && tk.getDoTinCayNguoiDung() < 5);
        if (!isAdmin && (tk.getTrangThai() == UserStatus.LOCKED || isLockedByPoint)) {
            saveSystemLog(tk, "LOGIN_BLOCKED", "Tài khoản bị khóa đăng nhập.");
            return ApiResponse.<AuthResponse>error(403, "Tài khoản của bạn đã bị khóa do vi phạm quy định hoặc điểm tin cậy thấp!");
        }

        // Trạng thái hợp lệ thì mới tiến hành kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getMatKhau(), tk.getMatKhau())) {
            return ApiResponse.<AuthResponse>error(401, "Mật khẩu không chính xác!");
        }

        // Tiến hành tạo dữ liệu trả về
        UserManagementResponse userDto = modelMapper.map(tk, UserManagementResponse.class);
        userDto.setVaiTro(roles);

        String accessToken = jwtService.generateToken(tk);
        String refreshToken = jwtService.generateRefreshToken(tk);

        AuthResponse authData = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userDto)
                .build();

        saveSystemLog(tk, "LOGIN_SUCCESS", "Người dùng [" + tk.getTenDangNhap() + "] đã đăng nhập thành công.");

        return ApiResponse.success("Đăng nhập thành công!", authData);
    }

    private void saveSystemLog(TaiKhoan taiKhoan, String hanhDong, String moTa) {
        try {
            NhatKyHeThong sysLog = new NhatKyHeThong();
            sysLog.setTaiKhoan(taiKhoan);
            sysLog.setHanhDong(hanhDong);
            sysLog.setMoTa(moTa);
            sysLog.setDiaChiIp("127.0.0.1");

            nhatKyHeThongRepository.save(sysLog);
        } catch (Exception e) {
            System.err.println("Lỗi lưu nhật ký hệ thống ngầm: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<ResetPasswordResponse> forgotPassword(String email) {
        // Kiểm tra xem Email có tồn tại trong hệ thống không
        TaiKhoan taiKhoan = taiKhoanRepository.findByEmail(email).orElse(null);

        if (taiKhoan == null) {
            return ApiResponse.error(404, AuthMessage.EMAIL_NOT_FOUND.getMessage());
        }

        // Tạo mã OTP
        String otp = String.valueOf((int)((Math.random() * 899999) + 100000));

        // Lưu thông tin vào bảng QuenMatKhau
        QuenMatKhau qmk = new QuenMatKhau();
        qmk.setEmail(email);
        qmk.setOtpCode(otp);
        qmk.setThoiGianHetHan(LocalDateTime.now().plusMinutes(5));
        qmk.setDaSuDung(false);
        qmk.setTaiKhoan(taiKhoan);

        quenMatKhauRepository.save(qmk);

        // Gửi Mail
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailSender);
            message.setTo(email);
            message.setSubject("MÃ XÁC THỰC QUÊN MẬT KHẨU");
            message.setText("Chào bạn,\n\nMã OTP để khôi phục mật khẩu của bạn là: " + otp +
                    "\n\nMã này sẽ hết hạn sau 5 phút. Vui lòng không cung cấp mã này cho bất kỳ ai.");

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi gửi mail: " + e.getMessage());
        }

        // Trả về Response Object kèm thông tin thời gian hết hạn
        ResetPasswordResponse responseData = ResetPasswordResponse.builder()
                .email(email)
                .thoiGianHetHan(qmk.getThoiGianHetHan())
                .message(AuthMessage.OTP_SENT.getMessage())
                .build();

        return ApiResponse.success(AuthMessage.OTP_SENT.getMessage(), null);
    }

    @Override
    @Transactional
    public ApiResponse<String> resetPassword(ResetPasswordRequest request) {
        // Tìm mã OTP mới nhất của email này và chưa được sử dụng
        QuenMatKhau qmk = quenMatKhauRepository
                .findFirstByEmailAndDaSuDungFalseOrderByThoiGianHetHanDesc(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Yêu cầu không hợp lệ hoặc đã hết hạn!"));

        // Kiểm tra mã OTP có khớp không
        if (!qmk.getOtpCode().equals(request.getOtp())) {
            return ApiResponse.error(400, "Mã OTP không chính xác!");
        }

        // Kiểm tra mã có bị hết hạn không
        if (qmk.getThoiGianHetHan().isBefore(LocalDateTime.now())) {
            return ApiResponse.error(400, "Mã OTP đã hết hạn!");
        }

        // Cập nhật mật khẩu mới cho tài khoản
        TaiKhoan taiKhoan = qmk.getTaiKhoan();
        taiKhoan.setMatKhau(passwordEncoder.encode(request.getMatKhauMoi()));
        taiKhoanRepository.save(taiKhoan);

        // Đánh dấu mã OTP này đã được sử dụng
        qmk.setDaSuDung(true);
        quenMatKhauRepository.save(qmk);

        return ApiResponse.success("Đổi mật khẩu mới thành công! Vui lòng đăng nhập lại.", null);
    }

    @Override
    @Transactional
    public ApiResponse<UserManagementResponse> createAdminAccount(RegisterRequest request) {
        // Kiểm tra trùng tên đăng nhập
        if (taiKhoanRepository.existsByTenDangNhap(request.getTenDangNhap())) {
            return ApiResponse.error(400, "Tên đăng nhập admin này đã tồn tại!");
        }

        // Tạo thực thể TaiKhoan
        TaiKhoan adminNew = new TaiKhoan();
        adminNew.setTenDangNhap(request.getTenDangNhap());
        adminNew.setMatKhau(passwordEncoder.encode(request.getMatKhau()));
        adminNew.setEmail(request.getEmail());
        adminNew.setHoTen(request.getHoTen());
        adminNew.setTrangThai(UserStatus.ACTIVE);

        TaiKhoan savedAdmin = taiKhoanRepository.save(adminNew);

        // Gán quyền ROLE_ADMIN
        VaiTro adminRole = vaiTroRepository.findByTenVaiTro(RoleConstant.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy vai trò ADMIN."));

        PhanQuyen pq = new PhanQuyen();
        pq.setTaiKhoan(savedAdmin);
        pq.setVaiTro(adminRole);
        phanQuyenRepository.save(pq);

        // Trả về thông tin
        UserManagementResponse response = modelMapper.map(savedAdmin, UserManagementResponse.class);
        response.setVaiTro(List.of(RoleConstant.ROLE_ADMIN));

        return ApiResponse.success("Đã tạo tài khoản Admin thành công!", response);
    }

    @Override
    public ApiResponse<AuthResponse> changePassword(ChangePasswordRequest request) {
        // Tìm tài khoản trong DB
        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(request.getTenDangNhap())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Kiểm tra mật khẩu cũ có khớp không
        if (!passwordEncoder.matches(request.getMatKhauCu(), taiKhoan.getMatKhau())) {
            return ApiResponse.error(400, "Mật khẩu cũ không chính xác!");
        }

        // Mã hóa mật khẩu mới và cập nhật
        taiKhoan.setMatKhau(passwordEncoder.encode(request.getMatKhauMoi()));
        taiKhoanRepository.save(taiKhoan);

        return ApiResponse.success("Đổi mật khẩu thành công!", null);
    }

    @Override
    public ApiResponse<AuthResponse> refreshToken(String refreshToken) {
        try {
            // Lấy username từ Refresh Token
            String username = jwtService.extractUsername(refreshToken);

            if (username != null) {
                var user = taiKhoanRepository.findByTenDangNhap(username)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                // Kiểm tra Refresh Token còn hạn không
                if (!jwtService.isTokenExpired(refreshToken)) {
                    // Tạo Access Token mới
                    String newAccessToken = jwtService.generateToken(user);

                    // Tạo UserResponse
                    UserManagementResponse userDto = UserManagementResponse.builder()
                            .taiKhoanId(user.getTaiKhoanId())
                            .tenDangNhap(user.getTenDangNhap())
                            .email(user.getEmail())
                            .hoTen(user.getHoTen())
                            .soDienThoai(user.getSoDienThoai())
                            .vaiTro(user.getDanhSachPhanQuyen().stream()
                                    .map(quyen -> quyen.getPhanQuyenId().toString())
                                    .toList())
                            .build();

                    // Trả về AuthResponse
                    AuthResponse response = AuthResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(refreshToken)
                            .user(userDto)
                            .build();

                    return ApiResponse.success("Làm mới Token thành công", response);
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi Refresh Token: " + e.getMessage());
            return ApiResponse.error(401, "Refresh Token không hợp lệ hoặc đã hết hạn!");
        }
        return ApiResponse.error(401, "Không thể làm mới Token");
    }

    @Override
    public ApiResponse<Void> logout(String token) {
        try {
            String pureToken = token.startsWith("Bearer ") ? token.substring(7) : token;

            String username = jwtService.extractUsername(pureToken);

            return ApiResponse.success("Tạm biệt " + username, null);
        } catch (ExpiredJwtException e) {
            return ApiResponse.success("Đăng xuất thành công (Token đã hết hạn trước đó)", null);
        } catch (Exception e) {
            return ApiResponse.error(400, "Lỗi đăng xuất");
        }
    }
}
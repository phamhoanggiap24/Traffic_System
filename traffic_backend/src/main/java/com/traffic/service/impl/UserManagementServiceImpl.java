package com.traffic.service.impl;

import com.traffic.common.UserStatus;
import com.traffic.dto.response.UserManagementResponse;
import com.traffic.entity.TaiKhoan;
import com.traffic.repository.TaiKhoanRepository;
import com.traffic.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Override
    public Page<UserManagementResponse> getAllUsers(String tenDangNhap, Pageable pageable) {
        Page<TaiKhoan> userPage;

        if (tenDangNhap != null && !tenDangNhap.trim().isEmpty()) {
            userPage = taiKhoanRepository.findByTenDangNhapContainingIgnoreCaseAndTrangThaiNot(tenDangNhap.trim(), UserStatus.INACTIVE, pageable);
        } else {
            userPage = taiKhoanRepository.findByTrangThaiNot(UserStatus.INACTIVE, pageable);
        }

        return userPage.map(user -> {
            UserManagementResponse dto = new UserManagementResponse();
            dto.setTaiKhoanId(user.getTaiKhoanId());
            dto.setTenDangNhap(user.getTenDangNhap());
            dto.setHoTen(user.getHoTen());
            dto.setEmail(user.getEmail());
            dto.setDoTinCayNguoiDung(user.getDoTinCayNguoiDung() != null ? user.getDoTinCayNguoiDung() : 50);
            dto.setSoDienThoai(user.getSoDienThoai());

            if (user.getDoTinCayNguoiDung() != null && user.getDoTinCayNguoiDung() < 5) {
                dto.setTrangThaiTaiKhoan(UserStatus.LOCKED);
            } else {
                dto.setTrangThaiTaiKhoan(UserStatus.ACTIVE);
            }

            if (user.getDanhSachPhanQuyen() != null) {
                List<String> roles = user.getDanhSachPhanQuyen().stream()
                        .map(pq -> pq.getVaiTro().getTenVaiTro())
                        .collect(Collectors.toList());
                dto.setVaiTro(roles);
            } else {
                dto.setVaiTro(new ArrayList<>());
            }

            return dto;
        });
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        TaiKhoan user = taiKhoanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản để xóa"));

        user.setTrangThai(UserStatus.INACTIVE);
        taiKhoanRepository.save(user);

        System.out.println("Đã xóa tài khoản ID: " + id);
    }

    @Override
    @Transactional
    public void lockUser(Long id) {
        TaiKhoan user = taiKhoanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng"));

        user.setDoTinCayNguoiDung(0);
        user.setTrangThai(UserStatus.LOCKED);

        taiKhoanRepository.save(user);
    }

    @Override
    @Transactional
    public void unlockUser(Long id) {
        TaiKhoan user = taiKhoanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng"));

        user.setDoTinCayNguoiDung(10);
        taiKhoanRepository.save(user);
    }
}
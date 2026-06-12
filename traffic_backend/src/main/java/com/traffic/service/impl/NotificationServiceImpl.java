package com.traffic.service.impl;

import com.traffic.common.ReadStatus;
import com.traffic.dto.response.NotificationResponse;
import com.traffic.entity.CanhBao;
import com.traffic.repository.CanhBaoRepository;
import com.traffic.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private CanhBaoRepository canhBaoRepository;

    private NotificationResponse convertToResponseDTO(CanhBao entity, boolean isAdmin) {
        String finalNoiDung = entity.getNoiDung();

        if (entity.getBaoCaoSuCo() != null) {
            String strLoaiSuCo = "Sự cố giao thông";
            if (entity.getBaoCaoSuCo().getLoaiSuCo() != null) {
                String tenGoc = entity.getBaoCaoSuCo().getLoaiSuCo().getTenLoai();
                if (tenGoc != null) {
                    strLoaiSuCo = tenGoc;
                }
            }

            String strTenDangNhap = "ẩn danh";
            if (entity.getBaoCaoSuCo().getTaiKhoan() != null) {
                String usernameGoc = entity.getBaoCaoSuCo().getTaiKhoan().getTenDangNhap();
                if (usernameGoc != null) {
                    strTenDangNhap = usernameGoc;
                }
            }

            if (isAdmin) {
                finalNoiDung = "Sự cố mới cần duyệt: Hệ thống nhận được báo cáo [" + strLoaiSuCo + "] từ " + strTenDangNhap + ".";
            }
        }

        return NotificationResponse.builder()
                .canhBaoId(entity.getCanhBaoId())
                .noiDung(finalNoiDung)
                .loaiCanhBao(entity.getLoaiCanhBao())
                .kenhGui(entity.getKenhGui())
                .trangThai(entity.getTrangThai() != null ? entity.getTrangThai() : ReadStatus.CHUA_DOC)
                .thoiGianGui(entity.getThoiGianGui())
                .taiKhoanId(entity.getTaiKhoan() != null ? entity.getTaiKhoan().getTaiKhoanId() : null)
                .tenNguoiNhan(entity.getTaiKhoan() != null ? entity.getTaiKhoan().getHoTen() : null)
                .baoCaoId(entity.getBaoCaoSuCo() != null ? entity.getBaoCaoSuCo().getBaoCaoId() : null)
                .build();
    }

    @Override
    public List<NotificationResponse> getBellNotifications(Long taiKhoanId, String vaiTro) {
        List<CanhBao> danhSachEntity;

        boolean isAdmin = vaiTro != null && ("ROLE_ADMIN".equals(vaiTro) || vaiTro.toUpperCase().contains("ADMIN"));

        if (isAdmin) {
            danhSachEntity = canhBaoRepository.findNotificationsForAdmin();
        } else {
            danhSachEntity = canhBaoRepository.findByTaiKhoanId(taiKhoanId);
        }

        return danhSachEntity.stream()
                .map(entity -> this.convertToResponseDTO(entity, isAdmin))
                .collect(Collectors.toList());
    }

    @Override
    public boolean markAsRead(Long canhBaoId) {
        Optional<CanhBao> optional = canhBaoRepository.findById(canhBaoId);
        if (optional.isPresent()) {
            CanhBao cb = optional.get();
            cb.setTrangThai(ReadStatus.DA_DOC);
            canhBaoRepository.save(cb);
            return true;
        }
        return false;
    }

    @Override
    public void markAllAsRead(Long taiKhoanId, String vaiTro) {
        List<CanhBao> danhSach;
        if ("ROLE_ADMIN".equals(vaiTro) || vaiTro.contains("ADMIN")) {
            danhSach = canhBaoRepository.findNotificationsForAdmin();
        } else {
            danhSach = canhBaoRepository.findByTaiKhoanId(taiKhoanId);
        }
        danhSach.forEach(cb -> {
            cb.setTrangThai(ReadStatus.DA_DOC);
            canhBaoRepository.save(cb);
        });
    }

    @Override
    public boolean deleteNotification(Long canhBaoId) {
        if (canhBaoRepository.existsById(canhBaoId)) {
            canhBaoRepository.deleteById(canhBaoId);
            return true;
        }
        return false;
    }

    @Override
    public void deleteAllNotifications(Long taiKhoanId, String vaiTro) {
        List<CanhBao> danhSach;
        if ("ROLE_ADMIN".equals(vaiTro) || vaiTro.contains("ADMIN")) {
            danhSach = canhBaoRepository.findNotificationsForAdmin();
        } else {
            danhSach = canhBaoRepository.findByTaiKhoanId(taiKhoanId);
        }
        canhBaoRepository.deleteAll(danhSach);
    }
}
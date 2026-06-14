/**
 * Utility functions cho format thời gian với timezone Việt Nam (UTC+7)
 */

/**
 * Format thời gian đầy đủ (ngày, tháng, năm, giờ, phút, giây)
 * @param {string|Date} dateString - Chuỗi thời gian hoặc Date object
 * @returns {string} Thời gian format theo timezone Asia/Ho_Chi_Minh
 */
export const formatTimeWithTimezone = (dateString) => {
  try {
    const date = new Date(dateString);

    const vnDate = new Date(date.getTime() + (7 * 60 * 60 * 1000));

    const formatter = new Intl.DateTimeFormat('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      timeZone: 'Asia/Ho_Chi_Minh'
    });

    return formatter.format(vnDate);
  } catch (err) {
    console.error('Lỗi format time:', err);
    return dateString;
  }
};

/**
 * Format chỉ lấy giờ và phút
 * @param {string|Date} dateString - Chuỗi thời gian hoặc Date object
 * @returns {string} Format HH:mm
 */
export const formatTimeOnly = (dateString) => {
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return dateString;
    }
    const formatter = new Intl.DateTimeFormat('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'Asia/Ho_Chi_Minh'
    });
    return formatter.format(date);
  } catch (err) {
    console.error('Lỗi format time:', err);
    return dateString;
  }
};

/**
 * Format chỉ lấy ngày, tháng, năm
 * @param {string|Date} dateString - Chuỗi thời gian hoặc Date object
 * @returns {string} Format DD/MM/YYYY
 */
export const formatDateOnly = (dateString) => {
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return dateString;
    }
    const formatter = new Intl.DateTimeFormat('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      timeZone: 'Asia/Ho_Chi_Minh'
    });
    return formatter.format(date);
  } catch (err) {
    console.error('Lỗi format date:', err);
    return dateString;
  }
};

/**
 * Tính thời gian cách từ bây giờ (nhân dân hóa)
 * @param {string|Date} dateString - Chuỗi thời gian hoặc Date object
 * @returns {string} Ví dụ: "5 phút trước", "2 giờ trước", "3 ngày trước"
 */
export const getRelativeTime = (dateString) => {
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      return dateString;
    }

    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSeconds = Math.floor(diffMs / 1000);
    const diffMinutes = Math.floor(diffSeconds / 60);
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffSeconds < 60) {
      return 'Vừa xong';
    } else if (diffMinutes < 60) {
      return `${diffMinutes} phút trước`;
    } else if (diffHours < 24) {
      return `${diffHours} giờ trước`;
    } else if (diffDays < 7) {
      return `${diffDays} ngày trước`;
    } else {
      return formatDateOnly(dateString);
    }
  } catch (err) {
    console.error('Lỗi tính relative time:', err);
    return dateString;
  }
};

import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMapEvents, Tooltip, Polyline } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './TrafficMap.css';
import ReactDOM from 'react-dom';
import { Navigation, Search, CheckCircle, Trash2, AlertTriangle, XCircle, MapPin, EyeOff, Image, X } from 'lucide-react';
import ReportForm from '../../user/ReportForm/ReportForm';
import api from '../../../api/axiosConfig';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const incidentIcons = {
  1: new L.Icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
    iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png', shadowSize: [41, 41]
  }),
  2: new L.Icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-gold.png',
    iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png', shadowSize: [41, 41]
  }),
  3: new L.Icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-orange.png',
    iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png', shadowSize: [41, 41]
  }),
  4: new L.Icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
    iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png', shadowSize: [41, 41]
  })
};

const startIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34]
});

const endIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34]
});

// Hàm format thời gian theo múi giờ Việt Nam khớp định dạng với danh sách quản trị
const formatVietnameseDateTime = (dateTimeString) => {
  if (!dateTimeString) return "Chưa xác định";
  try {
    const date = new Date(dateTimeString);
    const formatter = new Intl.DateTimeFormat('vi-VN', {
      timeZone: 'Asia/Ho_Chi_Minh',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour12: false
    });

    const parts = formatter.formatToParts(date);
    const hour = parts.find(p => p.type === 'hour').value;
    const minute = parts.find(p => p.type === 'minute').value;
    const second = parts.find(p => p.type === 'second').value;
    const day = parts.find(p => p.type === 'day').value;
    const month = parts.find(p => p.type === 'month').value;
    const year = parts.find(p => p.type === 'year').value;

    return `${hour}:${minute}:${second} ${day}/${month}/${year}`;
  } catch (error) {
    console.error("Lỗi định dạng ngày tháng:", error);
    return "Chưa xác định";
  }
};

// ==================== CÁC HÀM TRỢ GIÚP LỌC BÁN KÍNH 50M (MỚI THÊM) ====================

// Hàm tính khoảng cách mét giữa 2 điểm GPS (Công thức Haversine)
const getDistanceInMeters = (lat1, lon1, lat2, lon2) => {
  const R = 6371000; // Bán kính Trái Đất tính bằng mét
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
};

// Hàm lọc gộp trùng: Tách lọc theo từng loại sự cố, trong bán kính 50m chỉ giữ lại tin mới nhất để làm mới thời gian
const filterDuplicateIncidentsByRadius = (rawIncidents) => {
  if (!rawIncidents || rawIncidents.length === 0) return [];

  // Sắp xếp mảng thô từ MỚI NHẤT xuống CŨ NHẤT dựa trên thoiGianBaoCao
  const sortedIncidents = [...rawIncidents].sort((a, b) => {
    return new Date(b.thoiGianBaoCao).getTime() - new Date(a.thoiGianBaoCao).getTime();
  });

  const uniqueIncidents = [];

  sortedIncidents.forEach((currentIncident) => {
    // Kiểm tra xem trong danh sách "đã được duyệt giữ lại hiển thị" có điểm nào bị trùng lặp không
    const isDuplicate = uniqueIncidents.some((savedIncident) => {
      // ĐIỀU KIỆN 1: Phải TRÙNG LOẠI SỰ CỐ (loaiSuCoId) thì mới xét gộp. Khác loại cho hiện song song bình thường.
      const isSameType = savedIncident.loaiSuCoId === currentIncident.loaiSuCoId;

      if (isSameType) {
        // ĐIỀU KIỆN 2: Tính khoảng cách hình học thực tế xem có thuộc bán kính dưới 50m không
        const dist = getDistanceInMeters(
          savedIncident.viDo, savedIncident.kinhDo,
          currentIncident.viDo, currentIncident.kinhDo
        );
        return dist <= 50; // Khoảng cách <= 50m đánh dấu trùng lặp
      }
      return false;
    });

    // Do danh sách chạy từ mới nhất xuống cũ nhất, phần tử quét đầu tiên luôn là MỚI NHẤT tại khu vực bán kính đó.
    // Nếu chưa bị chiếm vị trí bởi sự cố mới nào cùng loại trước đó -> Tiến hành lưu lại để vẽ Marker.
    if (!isDuplicate) {
      uniqueIncidents.push(currentIncident);
    }
  });

  return uniqueIncidents;
};

const IncidentPopupContent = ({ data, userRole, fetchAddress, handleAdminAction }) => {
  const [address, setAddress] = useState("Đang xác định vị trí...");
  const [showPreviewModal, setShowPreviewModal] = useState(false);
  const isAdmin = userRole === 'ADMIN';

  // Lắng nghe dữ liệu ngầm thay đổi để cập nhật giao diện popup tức thì
  const [currentData, setCurrentData] = useState(data);

  useEffect(() => {
    setCurrentData(data);
  }, [data]);

  useEffect(() => {
    let isMounted = true;
    const lat = currentData.viDo || currentData.lat;
    const lng = currentData.kinhDo || currentData.lng;

    if (lat && lng) {
      fetchAddress(lat, lng).then(res => {
        if (isMounted) setAddress(res);
      });
    }
    return () => { isMounted = false; };
  }, [currentData, fetchAddress]);

  const reportTime = currentData.thoiGianBaoCao ? new Date(currentData.thoiGianBaoCao).getTime() : new Date().getTime();
  const currentTime = new Date().getTime();

  let expireMinutes = 5;
  if (currentData.loaiSuCoId === 1 || currentData.loaiSuCoId === 2) {
    expireMinutes = 5;
  }
  const isTimeOut = (currentTime - reportTime) > (expireMinutes * 60 * 1000);
  const isExpired = (isTimeOut && (currentData.trangThai === 'CHO_XAC_MINH' || currentData.trangThai === 'NGHI_VAN')) || currentData.trangThai === 'QUA_HAN';
  const isProcessed = currentData.trangThai === 'DA_XAC_MINH' || currentData.trangThai === 'SAI_SU_THAT';
  const isHiddenFromMap = currentData.trangThai === 'AN_HIEN_THI';

  const BACKEND_URL = 'https://traffic-backend-v2.onrender.com';
  const getImageUrl = (url) => {
    if (!url) return null;
    if (url.startsWith('http')) return url;
    return `${BACKEND_URL}${url}`;
  };
  const imageUrl = getImageUrl(currentData.hinhAnhUrl);

  return (
    <div className="map-popup-wrapper">
      <div className={isAdmin ? "admin-popup-header" : "user-popup-header"}>
        <AlertTriangle size={16} /> {isAdmin ? "QUẢN TRỊ SỰ CỐ" : "THÔNG TIN SỰ CỐ"}
      </div>

      <div className="map-popup-scrollable-body">
        {isExpired ? (
          <div className="expired-content-minimal">
            <p><strong>Vị trí đã chọn</strong></p>
            <p>{address}</p>
            <div className="admin-expired-notice" style={{ color: '#dc3545', fontSize: '11px', fontStyle: 'italic', marginTop: '5px' }}>
              Báo cáo đã quá thời hạn hiển thị thực tế
            </div>
          </div>
        ) : (
          <>
            <p><strong>Loại sự cố:</strong> {currentData.tenLoaiSuCo || "Không xác định"}</p>
            {isAdmin && (
              <p><strong>Trạng thái hệ thống:</strong> {
                currentData.trangThai === 'DA_XAC_MINH' ? "Tin thật" :
                currentData.trangThai === 'SAI_SU_THAT' ? "Tin giả" :
                currentData.trangThai === 'NGHI_VAN' ? "Nghi vấn" :
                currentData.trangThai === 'AN_HIEN_THI' ? "Đã ẩn bản đồ" : "Chờ duyệt"
              }</p>
            )}
            <p><strong>Mô tả:</strong> {currentData.moTa || "Không có mô tả"}</p>
            <p><strong>Vị trí:</strong> {address}</p>
            {/* Đã cập nhật chuyển sang hiển thị ngày giờ chuẩn Việt Nam */}
            <p><strong>Thời gian:</strong> {formatVietnameseDateTime(currentData.thoiGianBaoCao)}</p>

            {imageUrl && (
              <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <strong>Hình ảnh minh họa:</strong>
                <button
                  type="button"
                  className="btn-view-map"
                  style={{ padding: '3px 8px', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                  onClick={() => setShowPreviewModal(true)}
                >
                  <Image size={12} /> Xem ảnh
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {isAdmin && !isExpired && (
        <div className="admin-popup-actions">
          {!isProcessed && !isHiddenFromMap ? (
            <div className="admin-action-buttons-group">
              <button className="btn-map-approve" onClick={() => handleAdminAction(currentData.id || currentData.baoCaoId, 'approve')}>
                <CheckCircle size={14} /> Duyệt
              </button>
              <button className="btn-map-reject" onClick={() => handleAdminAction(currentData.id || currentData.baoCaoId, 'reject')}>
                <XCircle size={14} /> Từ chối
              </button>
            </div>
          ) : isHiddenFromMap ? (
            <button className="btn-map-delete-red" onClick={() => handleAdminAction(currentData.id || currentData.baoCaoId, 'delete')} style={{ margin: '0 auto' }}>
              <Trash2 size={14} /> Xóa khỏi danh sách
            </button>
          ) : (
            <button
              className="btn-map-remove-orange"
              onClick={() => handleAdminAction(currentData.id || currentData.baoCaoId, 'removeFromMap')}
            >
              <EyeOff size={14} /> Gỡ khỏi bản đồ
            </button>
          )}
        </div>
      )}

      {showPreviewModal && ReactDOM.createPortal(
        <div className="image-modal-overlay" onClick={() => setShowPreviewModal(false)} style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex',
          justifyContent: 'center', alignItems: 'center', zIndex: 9999
        }}>
          <div className="image-modal-content" onClick={(e) => e.stopPropagation()}>
            <button className="close-modal" onClick={() => setShowPreviewModal(false)}>×</button>
            <img src={imageUrl} alt="Sự cố giao thông thực tế" style={{ maxWidth: '100%', maxHeight: '80vh' }} />
          </div>
        </div>,
        document.body
      )}
    </div>
  );
};

const TrafficMap = () => {
  const [selectedPoint, setSelectedPoint] = useState(null);
  const [showReportModal, setShowReportModal] = useState(false);
  const [incidents, setIncidents] = useState([]);
  const [focusIncident, setFocusIncident] = useState(null);
  const [startSearch, setStartSearch] = useState('');
  const [endSearch, setEndSearch] = useState('');
  const [startPoint, setStartPoint] = useState(null);
  const [endPoint, setEndPoint] = useState(null);
  const [suggestions, setSuggestions] = useState({ type: '', data: [] });
  const [routesData, setRoutesData] = useState([]);
  const [activeRouteIndex, setActiveRouteIndex] = useState(0);
  const [, setTimeUpdate] = useState(0);
  const [isFirstLoad, setIsFirstLoad] = useState(true);
  const [mapZoom, setMapZoom] = useState(13);
  const MIN_INCIDENT_ZOOM = 12;

  const [activeInput, setActiveInput] = useState('start');
  const activeInputRef = useRef('start');

  const updateActiveInput = (val) => {
    setActiveInput(val);
    activeInputRef.current = val;
  };

  const debounceTimer = useRef(null);
  const polylineRefs = useRef({});

  const clearStartPoint = () => {
    setStartSearch('');
    setStartPoint(null);
    setRoutesData([]);
    setActiveRouteIndex(0);
    setSuggestions({ type: '', data: [] });
  };

  const clearEndPoint = () => {
    setEndSearch('');
    setEndPoint(null);
    setRoutesData([]);
    setActiveRouteIndex(0);
    setSuggestions({ type: '', data: [] });
  };

  const userRole = useMemo(() => {
    try {
      const userJson = localStorage.getItem('user');
      if (!userJson) return 'USER';
      const userObj = JSON.parse(userJson);
      return (userObj.vaiTro || []).includes('ROLE_ADMIN') ? 'ADMIN' : 'USER';
    } catch (e) { return 'USER'; }
  }, []);

  const mapRef = useRef();
  const markerRef = useRef(null);

  const fetchIncidents = useCallback(async () => {
    try {
      const res = await api.get(`/report/public/markers?_t=${new Date().getTime()}`);
      if (res.data?.data) setIncidents(res.data.data);
    } catch (err) { console.error("Lỗi tải markers:", err); }
  }, []);

  const calculateAndDrawRoutes = async (start, end) => {
    if (!start || !end) return;
    try {
      const response = await api.get('/traffic/route-eta', {
        params: {
          sLat: start.lat,
          sLng: start.lng,
          eLat: end.lat,
          eLng: end.lng
        }
      });

      const apiResponse = response.data;
      if (apiResponse && apiResponse.status === 200 && apiResponse.data?.routes) {
        const allRoutes = apiResponse.data.routes;
        setRoutesData(allRoutes);
        setActiveRouteIndex(0);

        if (mapRef.current && allRoutes[0]?.geometry) {
          const points = allRoutes[0].geometry.coordinates.map(coord => [coord[1], coord[0]]);
          mapRef.current.fitBounds(points, { padding: [50, 50] });
        }
      } else {
        console.warn("Không lấy được danh sách tuyến đường:", apiResponse?.message);
      }
    } catch (error) {
      console.error("Lỗi định tuyến đa tuyến thời gian thực:", error);
    }
  };

  const fetchAddress = useCallback(async (lat, lng) => {
    try {
      const wrapped = L.latLng(lat, lng).wrap();
      const response = await fetch(
        `https://nominatim.openstreetmap.org/reverse?lat=${wrapped.lat}&lon=${wrapped.lng}&format=json&addressdetails=1&zoom=18&accept-language=vi`
      );
      if (!response.ok) throw new Error("API Limit");
      const data = await response.json();

      if (data) {
        if (data.address) {
          const addr = data.address;
          const street = addr.road || addr.pedestrian || addr.suburb || addr.neighbourhood || addr.city_district || addr.quarter || addr.hamlet || "";
          const city = addr.city || addr.town || addr.village || addr.county || addr.state || "";

          if (street || city) {
            const houseNumber = addr.house_number ? `${addr.house_number} ` : "";
            return `${houseNumber}${street}${street && city ? ", " : ""}${city}`.trim();
          }
        }
        if (data.display_name) return data.display_name;
      }
      return `Vị trí tại: ${wrapped.lat.toFixed(5)}, ${wrapped.lng.toFixed(5)}`;
    } catch (err) {
      return `Vị trí: ${lat.toFixed(5)}, ${lng.toFixed(5)}`;
    }
  }, []);

  const handleInputChange = (value, type) => {
    if (type === 'start') {
      setStartSearch(value);
      if (!value.trim()) {
        setStartPoint(null);
        setRoutesData([]);
      }
    } else {
      setEndSearch(value);
      if (!value.trim()) {
        setEndPoint(null);
        setRoutesData([]);
      }
    }

    if (debounceTimer.current) clearTimeout(debounceTimer.current);

    if (value.trim().length < 2) {
      setSuggestions({ type: '', data: [] });
      return;
    }

    debounceTimer.current = setTimeout(async () => {
      try {
        let viewboxParam = "";
        if (mapRef.current) {
          const bounds = mapRef.current.getBounds();
          const sw = bounds.getSouthWest();
          const ne = bounds.getNorthEast();
          viewboxParam = `&viewbox=${sw.lng},${sw.lat},${ne.lng},${ne.lat}`;
        }

        const quốcTếRegex = /(paris|tokyo|london|singapore|new york|washington|laos|cambodia|thailand|usa|korea|japan)/i;
        const isNuocNgoai = quốcTếRegex.test(value);
        const countryFilter = isNuocNgoai ? "" : "&countrycodes=vn";

        const res = await fetch(
          `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(value.trim())}&limit=40&addressdetails=1&accept-language=vi${viewboxParam}${countryFilter}`
        );
        const data = await res.json();

        const currentBounds = mapRef.current?.getBounds();

        const sortedData = (data || []).sort((a, b) => {
          const aIsRoad = a.class === 'highway' ? 1 : 0;
          const bIsRoad = b.class === 'highway' ? 1 : 0;
          if (aIsRoad !== bIsRoad) return bIsRoad - aIsRoad;

          const aInView = currentBounds?.contains([parseFloat(a.lat), parseFloat(a.lon)]) ? 1 : 0;
          const bInView = currentBounds?.contains([parseFloat(b.lat), parseFloat(b.lon)]) ? 1 : 0;
          if (aInView !== bInView) return bInView - aInView;

          const aIsVN = a.address?.country_code === 'vn' ? 1 : 0;
          const bIsVN = b.address?.country_code === 'vn' ? 1 : 0;
          if (aIsVN !== bIsVN) return bIsVN - aIsVN;

          return (b.importance || 0) - (a.importance || 0);
        });

        setSuggestions({ type, data: sortedData.slice(0, 12) });
      } catch (error) {
        console.error("Lỗi lấy gợi ý:", error);
      }
    }, 450);
  };

  const handleSelectSuggestion = (item, type) => {
    const newPos = { lat: parseFloat(item.lat), lng: parseFloat(item.lon), address: item.display_name };
    if (type === 'start') {
      setStartPoint(newPos);
      setStartSearch(item.display_name);
      if (endPoint) calculateAndDrawRoutes(newPos, endPoint);
    } else {
      setEndPoint(newPos);
      setEndSearch(item.display_name);
      if (startPoint) calculateAndDrawRoutes(startPoint, newPos);
    }
    setSuggestions({ type: '', data: [] });
    mapRef.current.flyTo([newPos.lat, newPos.lng], 15);
  };

  const handleSearchLocation = async (query, type) => {
    if (!query.trim()) return;
    try {
      let viewboxParam = "";
      if (mapRef.current) {
        const bounds = mapRef.current.getBounds();
        const sw = bounds.getSouthWest();
        const ne = bounds.getNorthEast();
        viewboxParam = `&viewbox=${sw.lng},${sw.lat},${ne.lng},${ne.lat}`;
      }
      const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1&addressdetails=1&accept-language=vi${viewboxParam}&bounded=0`);
      const data = await res.json();
      if (data && data.length > 0) {
        const item = data[0];
        const newPos = { lat: parseFloat(item.lat), lng: parseFloat(item.lon), address: item.display_name };
        if (type === 'start') {
          setStartPoint(newPos);
          setStartSearch(item.display_name);
          if (endPoint) calculateAndDrawRoutes(newPos, endPoint);
        } else {
          setEndPoint(newPos);
          setEndSearch(item.display_name);
          if (startPoint) calculateAndDrawRoutes(startPoint, newPos);
        }
        setSuggestions({ type: '', data: [] });
        mapRef.current.flyTo([newPos.lat, newPos.lng], 15);
      } else { alert("Không tìm thấy địa chỉ!"); }
    } catch (error) { console.error("Lỗi tìm kiếm:", error); }
  };

  useEffect(() => {
    let timerId;
    const startPolling = async () => {
      if (document.visibilityState === 'visible') {
        await fetchIncidents();
      }
      timerId = setTimeout(startPolling, 5000);
    };

    startPolling();

    return () => {
      if (timerId) clearTimeout(timerId);
    };
  }, [fetchIncidents]);

  // Ra lệnh tự động định vị ngay khi MapContainer sẵn sàng ở luồng mount trang ban đầu
  useEffect(() => {
    if (mapRef.current) {
      mapRef.current.locate({
        enableHighAccuracy: true
      });
    }
  }, []);

  useEffect(() => {
    const interval = setInterval(() => {
      setTimeUpdate(prev => prev + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (selectedPoint && markerRef.current) { markerRef.current.openPopup(); }
  }, [selectedPoint]);

  useEffect(() => {
    if (routesData.length > 0 && polylineRefs.current[activeRouteIndex]) {
      const activePolyline = polylineRefs.current[activeRouteIndex];
      const routeGeom = routesData[activeRouteIndex]?.geometry?.coordinates;
      if (routeGeom && routeGeom.length > 0) {
        const middleIndex = Math.floor(routeGeom.length / 2);
        const midCoord = routeGeom[middleIndex];
        if (midCoord) {
          activePolyline.bringToFront();
          setTimeout(() => {
            activePolyline.openPopup([midCoord[1], midCoord[0]]);
          }, 150);
        }
      }
    }
  }, [activeRouteIndex, routesData]);

  useEffect(() => {
    const handleTriggerFly = () => {
      setTimeout(() => {
        const urlParams = new URLSearchParams(window.location.search);
        const urlLat = urlParams.get('lat');
        const urlLng = urlParams.get('lng');
        const focusData = localStorage.getItem('focusLocation');

        if (mapRef.current) {
          mapRef.current.invalidateSize();

          if (urlLat && urlLng) {
            const lat = parseFloat(urlLat);
            const lng = parseFloat(urlLng);
            mapRef.current.flyTo([lat, lng], 16);
            window.history.replaceState({}, document.title, window.location.pathname);
          }
          else if (focusData) {
            const item = JSON.parse(focusData);
            mapRef.current.flyTo([item.lat, item.lng], 16);
            setFocusIncident(item);
            localStorage.removeItem('focusLocation');
          }
        }
      }, 300);
    };

    handleTriggerFly();
    window.addEventListener('focus-accident', handleTriggerFly);
    return () => { window.removeEventListener('focus-accident', handleTriggerFly); };
  }, []);

  const handleAdminAction = async (id, action) => {
    try {
      if (action === 'delete') {
        if (!window.confirm("Xác nhận xóa báo cáo này khỏi danh sách?")) return;
        await api.delete(`/report/admin/xoa/${id}`);
      } else if (action === 'removeFromMap') {
        if (!window.confirm("Xác nhận gỡ sự cố này khỏi bản đồ?")) return;
        await api.patch(`/report/admin/go-khoi-map/${id}`);
      } else if (action === 'reject') {
        if (!window.confirm("Từ chối báo cáo?")) return;
        await api.patch(`/report/admin/xac-minh/${id}`, null, { params: { trangThaiMoi: 'SAI_SU_THAT' } });
      } else {
        await api.patch(`/report/admin/xac-minh/${id}`, null, { params: { trangThaiMoi: 'DA_XAC_MINH' } });
      }

      window.dispatchEvent(new Event('incident-verified'));
      setFocusIncident(null);
      fetchIncidents();
      alert("Xử lý thao tác thành công!");
    } catch (err) { alert("Lỗi khi thực hiện thao tác dữ liệu!"); }
  };

  function MapEventsHandler() {
    useMapEvents({
      click: async (e) => {
        if (e.originalEvent.defaultPrevented) return;
        setSuggestions({ type: '', data: [] });
        const wrapped = e.latlng.wrap();
        const addr = await fetchAddress(wrapped.lat, wrapped.lng);
        setSelectedPoint({ lat: wrapped.lat, lng: wrapped.lng, address: addr });
      },
      zoomend: (e) => {
        setMapZoom(e.target.getZoom());
      },
      locationfound: async (e) => {
        const wrapped = e.latlng.wrap();
        const token = localStorage.getItem('accessToken');

        if (token) {
          api.post('/profile/location', {
            viDo: wrapped.lat,
            kinhDo: wrapped.lng
          }).catch((err) => {
            console.warn('Không cập nhật được vị trí:', err.response?.status);
          });
        }

        // Mặc định ban đầu: Di chuyển tâm về vị trí hiện tại của user, không sinh popup, giữ nguyên độ rộng zoom
        if (isFirstLoad) {
          mapRef.current.panTo(wrapped);
          setIsFirstLoad(false);
          return;
        }

        try {
          const response = await fetch(
            `https://nominatim.openstreetmap.org/reverse?lat=${wrapped.lat}&lon=${wrapped.lng}&format=json&accept-language=vi`
          );
          const data = await response.json();
          const addr = data.display_name || `Vị trí tại: ${wrapped.lat.toFixed(5)}, ${wrapped.lng.toFixed(5)}`;

          const positionResult = { lat: wrapped.lat, lng: wrapped.lng, address: addr };
          setSelectedPoint(positionResult);

          const currentActive = activeInputRef.current;

          if (currentActive === 'start') {
            setStartPoint(positionResult);
            setStartSearch(addr);
            if (endPoint) calculateAndDrawRoutes(positionResult, endPoint);
          } else {
            setEndPoint(positionResult);
            setEndSearch(addr);
            if (startPoint) calculateAndDrawRoutes(startPoint, positionResult);
          }

          mapRef.current.flyTo(wrapped, 16);
        } catch (error) {
          console.error("Lỗi định vị:", error);
        }
      }
    });
    return null;
  }

  const handleLocate = () => {
    if (mapRef.current) {
      mapRef.current.locate({
        setView: true,
        maxZoom: 16,
        enableHighAccuracy: true
      });
    }
  };

  return (
    <div className="map-wrapper-fullscreen">
      <div className="routing-search-container">
        <div className="search-box-wrapper">
          <div className="search-input-group">
            <MapPin size={18} color="#10b981" className="search-icon-inline" />
            <input
              type="text"
              placeholder="Điểm bắt đầu..."
              value={startSearch}
              onFocus={() => updateActiveInput('start')}
              onChange={(e) => handleInputChange(e.target.value, 'start')}
              onKeyDown={(e) => e.key === 'Enter' && handleSearchLocation(startSearch, 'start')}
            />

            {startSearch && (
              <button type="button" className="clear-search-btn" onClick={clearStartPoint}>
                <X size={14} />
              </button>
            )}

            <button onClick={() => handleSearchLocation(startSearch, 'start')} className="inner-search-btn">
              <Search size={16} />
            </button>
            {suggestions.type === 'start' && suggestions.data.length > 0 && (
              <ul className="suggestion-list">
                {suggestions.data.map((item, index) => (
                  <li key={index} onClick={() => handleSelectSuggestion(item, 'start')}>
                    <MapPin size={14} /> {item.display_name}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="search-divider-vertical"></div>
          <div className="search-input-group">
            <MapPin size={18} color="#2563eb" className="search-icon-inline" />
            <input
              type="text"
              placeholder="Điểm đến..."
              value={endSearch}
              onFocus={() => updateActiveInput('end')}
              onChange={(e) => handleInputChange(e.target.value, 'end')}
              onKeyDown={(e) => e.key === 'Enter' && handleSearchLocation(endSearch, 'end')}
            />

            {endSearch && (
              <button type="button" className="clear-search-btn" onClick={clearEndPoint}>
                <X size={14} />
              </button>
            )}

            <button onClick={() => handleSearchLocation(endSearch, 'end')} className="inner-search-btn">
              <Search size={16} />
            </button>
            {suggestions.type === 'end' && suggestions.data.length > 0 && (
              <ul className="suggestion-list">
                {suggestions.data.map((item, index) => (
                  <li key={index} onClick={() => handleSelectSuggestion(item, 'end')}>
                    <MapPin size={14} /> {item.display_name}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
      <button className="current-location-btn-right" onClick={handleLocate} title="Vị trí hiện tại">
        <Navigation size={22} />
      </button>
      <MapContainer key="main-map" center={[21.0285, 105.8542]} zoom={13} className="full-map" ref={mapRef} zoomControl={false}>
        <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <MapEventsHandler />

        {startPoint && endPoint &&
          [...routesData].map((route, originalIndex) => {
            const pathPoints = route.geometry.coordinates.map(coord => [coord[1], coord[0]]);
            const isActive = originalIndex === activeRouteIndex;
            const durationMinutes = route.thoiGianUocTinhPhut;
            const distanceKm = (route.distanceMeters / 1000).toFixed(1);

            return (
              <Polyline
                key={`polyline-route-${originalIndex}`}
                positions={pathPoints}
                ref={(ref) => { if (ref) polylineRefs.current[originalIndex] = ref; }}
                eventHandlers={{
                  click: (e) => {
                    e.originalEvent.stopPropagation();
                    e.originalEvent.preventDefault();
                    setActiveRouteIndex(originalIndex);
                  }
                }}
                pathOptions={{
                  color: isActive ? '#2563eb' : '#93c5fd',
                  weight: isActive ? 9 : 5,
                  opacity: isActive ? 1.0 : 0.6,
                  lineJoin: 'round',
                  lineCap: 'round'
                }}
              >
                <Popup autoClose={false} closeOnClick={false}>
                  <div style={{ fontSize: '13px', minWidth: '180px' }}>
                    <strong style={{ color: isActive ? '#2563eb' : '#4b5563' }}>
                      Tuyến đường gợi ý {originalIndex + 1}:
                    </strong><br />
                    Quãng đường dài khoảng: <b>{distanceKm} km</b><br />
                    Thời gian ước tính khoảng: <b style={{ color: isActive ? '#dc3545' : '#2563eb' }}>{durationMinutes} phút</b>
                  </div>
                </Popup>
              </Polyline>
            );
          })
        }

        {startPoint && (
          <Marker position={[startPoint.lat, startPoint.lng]} icon={startIcon}>
            <Popup><strong>Điểm đi:</strong><br />{startPoint.address}</Popup>
          </Marker>
        )}
        {endPoint && (
          <Marker position={[endPoint.lat, endPoint.lng]} icon={endIcon}>
            <Popup><strong>Điểm đến:</strong><br />{endPoint.address}</Popup>
          </Marker>
        )}

        {/* Nhúng hàm filterDuplicateIncidentsByRadius làm bộ lọc lớp đầu tiên trước khi duyệt map */}
        {mapZoom >= MIN_INCIDENT_ZOOM && filterDuplicateIncidentsByRadius(incidents)
          .filter(i => {
            if (['SAI_SU_THAT', 'AN_HIEN_THI', 'DA_XOA', 'QUA_HAN'].includes(i.trangThai)) return false;

            try {
              const rTime = new Date(i.thoiGianBaoCao).getTime();
              const cTime = new Date().getTime();

              let limitMinutes = 15;
              if (i.loaiSuCoId === 1 || i.loaiSuCoId === 2) {
                limitMinutes = 10;
              }
              if ((cTime - rTime) > (limitMinutes * 60 * 1000)) {
                return false;
              }
            } catch (err) { return false; }

            if (focusIncident && i.baoCaoId === focusIncident.baoCaoId) return false;

            return true;
          })
          .map((incident) => {
            const level = Number(incident.mucDoUnTac || incident.mucDo || 4);
            const iconToRender = incidentIcons[level] || incidentIcons[4];

            return (
              <Marker
                key={incident.baoCaoId}
                position={[incident.viDo, incident.kinhDo]}
                icon={iconToRender}
              >
                <Tooltip permanent direction="top" offset={[0, -10]} className="incident-label">
                  {incident.tenLoaiSuCo}
                </Tooltip>

                {/* Khóa key kết hợp ID và TrangThai phản hồi để ép Leaflet vẽ lại ruột popup khi Admin duyệt hoặc làm mới thời gian */}
                <Popup minWidth={250} key={`${incident.baoCaoId}-${incident.trangThai}`}>
                  <IncidentPopupContent
                    data={incident}
                    userRole={userRole}
                    fetchAddress={fetchAddress}
                    handleAdminAction={handleAdminAction}
                  />
                </Popup>
              </Marker>
            );
          })}

        {userRole === 'ADMIN' && focusIncident && (
          <Marker
            position={[focusIncident.lat, focusIncident.lng]}
            icon={incidentIcons[Number(focusIncident.mucDo)] || incidentIcons[4]}
            zIndexOffset={1000}
            eventHandlers={{ add: (e) => e.target.openPopup() }}
          >
            <Popup minWidth={250} key={`${focusIncident.baoCaoId}-${focusIncident.trangThai}`}>
              <IncidentPopupContent
                data={focusIncident}
                userRole={userRole}
                fetchAddress={fetchAddress}
                handleAdminAction={handleAdminAction}
              />
            </Popup>
          </Marker>
        )}

        {selectedPoint && (
          <Marker position={[selectedPoint.lat, selectedPoint.lng]} ref={markerRef}>
            <Popup minWidth={200}>
              <div className="map-popup-content">
                <strong className="map-popup-title">Vị trí đã chọn</strong>
                <p className="map-popup-address">{selectedPoint.address}</p>
                {userRole !== 'ADMIN' ? (
                  <button className="popup-report-btn" onClick={() => setShowReportModal(true)} style={{ width: '100%', marginTop: '5px' }}>Báo cáo tại đây</button>
                ) : (
                  <div className="admin-view-mode-label">Chế độ xem quản trị (Admin)</div>
                )}
              </div>
            </Popup>
          </Marker>
        )}
      </MapContainer>
      {showReportModal && (<ReportForm selectedPoint={selectedPoint} onClose={() => { setShowReportModal(false); fetchIncidents(); }} />)}
    </div>
  );
};

export default TrafficMap;
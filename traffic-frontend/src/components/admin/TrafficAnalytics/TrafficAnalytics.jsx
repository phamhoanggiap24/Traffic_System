import React, { useState, useEffect, useRef, useCallback } from 'react';
import api from '../../../api/axiosConfig';
import { MapContainer, TileLayer, Marker, Popup, Circle, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { AlertTriangle, MapPin, Calendar, RefreshCw, Search, Navigation, Gauge } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell } from 'recharts';
import './TrafficAnalytics.css';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

const centerIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const TrafficAnalytics = () => {
  const [overview, setOverview] = useState({ totalIncidentsToday: 0, monitoredAreasCount: 0, lastSyncTime: "" });
  const [timeTrendData, setTimeTrendData] = useState([]);
  const [locationDensityData, setLocationDensityData] = useState([]);
  const [speedEvolutionData, setSpeedEvolutionData] = useState([]);

  const [loading, setLoading] = useState(true);

  // Bộ lọc dữ liệu kết hợp Thời gian + Không gian
  const [filters, setFilters] = useState({
    startDate: new Date(new Date().setDate(new Date().getDate() - 30)).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0],
    lat: null,
    lng: null,
    radius: 3000
  });

  // State quản lý việc tìm kiếm đường trên bản đồ phân tích
  const [searchQuery, setSearchQuery] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [addressName, setAddressName] = useState("Toàn thành phố");
  const mapRef = useRef();
  const debounceTimer = useRef(null);

  // Gọi API để lấy tên đường từ tọa độ
  const fetchAddressFromCoords = useCallback(async (lat, lng) => {
    try {
      const response = await fetch(`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json&accept-language=vi`);
      const data = await response.json();
      return data.display_name || `Tọa độ: ${lat.toFixed(4)}, ${lng.toFixed(4)}`;
    } catch {
      return `Tọa độ: ${lat.toFixed(4)}, ${lng.toFixed(4)}`;
    }
  }, []);

  // Kích hoạt định vị thiết bị trên bản đồ nhỏ
  const handleLocate = () => {
    if (mapRef.current) {
      mapRef.current.locate({ setView: true, maxZoom: 14 });
    }
  };

  // Xử lý tìm kiếm gợi ý tên đường
  const handleSearchChange = (value) => {
    setSearchQuery(value);
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    if (value.length < 3) {
      setSuggestions([]);
      return;
    }
    debounceTimer.current = setTimeout(async () => {
      try {
        const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(value)}&countrycodes=vn&limit=5&accept-language=vi`);
        const data = await res.json();
        setSuggestions(data || []);
      } catch (error) { console.error("Lỗi gợi ý:", error); }
    }, 500);
  };

  // Chọn địa điểm từ gợi ý tìm kiếm
  const handleSelectLocation = async (item) => {
    const lat = parseFloat(item.lat);
    const lng = parseFloat(item.lon);

    setAddressName(item.display_name);
    setFilters(prev => ({ ...prev, lat, lng }));
    setSuggestions([]);
    setSearchQuery('');

    if (mapRef.current) mapRef.current.flyTo([lat, lng], 14);
  };

  const handleClearLocation = () => {
    setAddressName("Toàn thành phố");
    setFilters(prev => ({ ...prev, lat: null, lng: null }));
    setSearchQuery('');
    setSuggestions([]);
  };

  // Component bắt điểm click và định vị trên bản đồ nhỏ
  function MapClickHandler() {
    useMapEvents({
      click: async (e) => {
        const { lat, lng } = e.latlng;
        const addr = await fetchAddressFromCoords(lat, lng);
        setAddressName(addr);
        setFilters(prev => ({ ...prev, lat, lng }));
      },
      locationfound: async (e) => {
        const { lat, lng } = e.latlng;
        const addr = await fetchAddressFromCoords(lat, lng);
        setAddressName(addr);
        setFilters(prev => ({ ...prev, lat, lng }));
      }
    });
    return null;
  }

  const fetchAnalyticsData = useCallback(async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('accessToken');
      const headers = { Authorization: `Bearer ${token}` };
      const params = {};

      const today = new Date();
      const thirtyDaysAgo = new Date(today);
      thirtyDaysAgo.setDate(today.getDate() - 30);

      const finalStartDate = filters.startDate ? filters.startDate : thirtyDaysAgo.toISOString().split('T')[0];
      const finalEndDate = filters.endDate ? filters.endDate : today.toISOString().split('T')[0];

      params.startDate = `${finalStartDate}T00:00:00`;
      params.endDate = `${finalEndDate}T23:59:59`;

      if (filters.lat !== null && filters.lng !== null) {
        params.lat = parseFloat(filters.lat);
        params.lng = parseFloat(filters.lng);
        params.radius = Number(filters.radius);
      }

      const [overviewRes, trendRes, locationRes, speedRes] = await Promise.all([
        api.get('/admin/analytics/overview', { params, headers }),
        api.get('/admin/analytics/incident-trend', { params, headers }),
        api.get('/admin/analytics/incident-location', { params, headers }),
        api.get('/admin/analytics/speed-evolution', { params, headers })
      ]);

      // Xử lý dữ liệu biểu đồ cột xu hướng sự cố
      const filledData = [];
      const start = new Date(finalStartDate);
      const end = new Date(finalEndDate);

      for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
        const dateStr = d.toISOString().split('T')[0];
        const existing = trendRes.data.find(item => item.reportDate === dateStr);
        filledData.push({
          reportDate: dateStr,
          totalReports: existing ? existing.totalReports : 0
        });
      }
      setTimeTrendData(filledData);

      // Cập nhật dữ liệu tổng quan và mật độ sự cố
      setOverview(overviewRes.data || { totalIncidentsToday: 0, monitoredAreasCount: 0, lastSyncTime: "" });
      setLocationDensityData(locationRes.data || []);

      // Xử lý dữ liệu biểu đồ vận tốc
      const full24HoursFrame = Array.from({ length: 24 }, (_, i) => ({
        hour: i,
        averageSpeed: 0
      }));

      if (filters.lat !== null && filters.lng !== null) {
        if (speedRes.data && speedRes.data.length > 0) {
          speedRes.data.forEach(item => {
            const targetHour = full24HoursFrame.find(h => h.hour === item.hour);
            if (targetHour) {
              const speedValue = item.averageSpeed !== undefined ? item.averageSpeed : (item.speed || 0);
              targetHour.averageSpeed = speedValue > 0 ? parseFloat(Number(speedValue).toFixed(1)) : 0;
            }
          });
        }
      }
      setSpeedEvolutionData(full24HoursFrame);

    } catch (error) {
      console.error("Lỗi khi tải dữ liệu phân tích theo vùng:", error);
    } finally {
      setLoading(false);
    }
  }, [filters.startDate, filters.endDate, filters.lat, filters.lng, filters.radius]);

  useEffect(() => {
    fetchAnalyticsData();
  }, [fetchAnalyticsData]);

  const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

  return (
    <div className="analytics-wrapper">

      {/* LỌC KẾT HỢP BẢN ĐỒ VÀ THỜI GIAN */}
      <div className="analytics-filter-container-split">

        {/* Bên Trái: Bản đồ chọn khu vực */}
        <div className="mini-map-filter-box">
          <div className="mini-map-header-search">
            <div className="search-map-group">
              <Search size={14} className="icon-search-map" />
              <input
                type="text"
                placeholder="Tìm tên đường, khu vực phân tích..."
                value={searchQuery}
                onChange={(e) => handleSearchChange(e.target.value)}
              />
              {filters.lat && (
                <button className="clear-location-tag-btn" onClick={handleClearLocation}>Xem toàn bộ</button>
              )}
              {suggestions.length > 0 && (
                <ul className="mini-map-suggestions">
                  {suggestions.map((item, idx) => (
                    <li key={idx} onClick={() => handleSelectLocation(item)}>{item.display_name}</li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          <div className="map-container-holder">
            <button
              type="button"
              className="mini-map-locate-btn-right"
              onClick={handleLocate}
              title="Vị trí hiện tại của bạn"
            >
              <Navigation size={18} />
            </button>

            <MapContainer key="analytics-map" center={[21.0285, 105.8542]} zoom={13} className="analytics-mini-map" ref={mapRef} zoomControl={false}>
              <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
              <MapClickHandler />
              {filters.lat && filters.lng && (
                <>
                  <Marker position={[filters.lat, filters.lng]} icon={centerIcon}>
                    <Popup><div style={{fontSize: '12px'}}>{addressName}</div></Popup>
                  </Marker>
                  <Circle
                    center={[filters.lat, filters.lng]}
                    radius={filters.radius}
                    pathOptions={{ color: '#ef4444', fillColor: '#ef4444', fillOpacity: 0.15 }}
                  />
                </>
              )}
            </MapContainer>
          </div>
        </div>

        {/* Bên Phải: Lọc thông số */}
        <div className="controls-filter-box">
          <h4 className="filter-section-title">Phạm vi thống kê sự cố</h4>

          <div className="current-focus-location-card">
            <MapPin size={16} className="text-rose" />
            <div className="focus-info">
              <span>Khu vực đang chọn:</span>
              <p className="truncated-text" title={addressName}>{addressName}</p>
            </div>
          </div>

          <div className="input-group">
            <label><Calendar size={14} /> Từ ngày</label>
            <input type="date" value={filters.startDate} onChange={(e) => setFilters({...filters, startDate: e.target.value})} />
          </div>

          <div className="input-group" style={{marginTop: '12px'}}>
            <label><Calendar size={14} /> Đến ngày</label>
            <input type="date" value={filters.endDate} onChange={(e) => setFilters({...filters, endDate: e.target.value})} />
          </div>

          <div className="input-group" style={{ marginTop: '12px' }}>
            <label><Gauge size={14} /> Bán kính quét xung quanh tâm</label>
            <select
              value={filters.radius}
              disabled={!filters.lat}
              onChange={(e) => setFilters(prev => ({ ...prev, radius: Number(e.target.value) }))}
            >
              <option value="1000">1 km (Lân cận gần)</option>
              <option value="3000">3 km (Phạm vi phường/quận)</option>
              <option value="5000">5 km (Phạm vi rộng)</option>
              <option value="10000">10 km (Toàn vùng đô thị)</option>
            </select>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="analytics-loading"><RefreshCw className="animate-spin" /> Đang tổng hợp số liệu vùng giao thông...</div>
      ) : (
        <>
          {/* TỔNG QUAN SỐ LIỆU */}
          <div className="overview-grid" style={{ gridTemplateColumns: 'repeat(3, minmax(0, 1fr))' }}>
            {/* Sự cố vùng chọn */}
            <div className="overview-card shadow-blue">
              <div className="icon-box" style={{ background: '#eff6ff', color: '#2563eb' }}><AlertTriangle size={24} /></div>
              <div className="card-content"><p>Sự cố vùng chọn</p><h3>{overview.totalIncidentsToday}</h3></div>
            </div>

            {/* Tổng số khu vực giám sát */}
            <div className="overview-card shadow-emerald">
              <div className="icon-box" style={{ background: '#ecfdf5', color: '#059669' }}><Navigation size={24} /></div>
              <div className="card-content">
                <p>Khu vực giám sát</p>
                <h3>
                  {overview.monitoredAreasCount || 0}
                  <span className="unit"> điểm</span>
                </h3>
              </div>
            </div>

            {/* Thời gian đồng bộ hệ thống */}
            <div className="overview-card shadow-rose">
              <div className="icon-box" style={{ background: '#fff1f2', color: '#e11d48' }}><RefreshCw size={24} /></div>
              <div className="card-content">
                <p>Đồng bộ gần nhất</p>
                <h3 style={{ fontSize: '20px', marginTop: '4px' }}>
                  {overview.lastSyncTime || "Vừa xong"}
                </h3>
              </div>
            </div>
          </div>

          {/* BIỂU ĐỒ CHÍNH */}
          <div className="charts-main-grid">
            <div className="chart-box">
              <div className="chart-header"><h4>Xu hướng lượng sự cố phát sinh tại khu vực</h4></div>
              <div className="chart-body">
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={timeTrendData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="reportDate" tick={{ fontSize: 11, fill: '#64748b' }} />
                    <YAxis tick={{ fontSize: 11, fill: '#64748b' }} allowDecimals={false}/>
                    <Tooltip />
                    <Bar dataKey="totalReports" name="Số lượng vụ việc" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="chart-box">
              <div className="chart-header"><h4>Biến động tốc độ dòng xe theo khung giờ (Khu vực)</h4></div>
              <div className="chart-body">
                <ResponsiveContainer width="100%" height={280}>
                  <LineChart data={speedEvolutionData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="hour" tick={{ fontSize: 11, fill: '#64748b' }} tickFormatter={(h) => `${h}h`} />
                    <YAxis tick={{ fontSize: 11, fill: '#64748b' }} domain={[0, 60]} allowDecimals={false}/>
                    <Tooltip />
                    <Line type="monotone" dataKey="averageSpeed" name="Tốc độ TB (km/h)" stroke="#10b981" strokeWidth={2.5} dot={{ r: 4 }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="chart-box full-width-chart">
              <div className="chart-header"><h4>Mật độ sự cố giao thông và Tỷ lệ xác minh theo Loại Sự Cố</h4></div>
              <div className="chart-body location-chart-flex">
                <ResponsiveContainer width="50%" height={280}>
                  <PieChart>
                    <Pie data={locationDensityData} cx="50%" cy="50%" innerRadius={60} outerRadius={90} paddingAngle={4} dataKey="totalReports" nameKey="locationName">
                      {locationDensityData.map((entry, index) => <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />)}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
                <div className="location-legend-list">
                  {locationDensityData && locationDensityData.length > 0 ? (
                    locationDensityData.map((item, index) => (
                      <div key={item.locationName} className="legend-row">
                        <span className="color-indicator" style={{ backgroundColor: COLORS[index % COLORS.length] }}></span>
                        <span className="location-name">{item.locationName}:</span>
                        <span className="location-value"><strong>{item.totalReports}</strong> vụ việc </span>
                        <span className="verified-tag">({item.verifiedReports} đã xác minh)</span>
                      </div>
                    ))
                  ) : (
                    <div style={{color: '#64748b', fontSize: '14px', padding: '20px'}}>Không có dữ liệu phân loại sự cố tại bán kính vùng này</div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default TrafficAnalytics;
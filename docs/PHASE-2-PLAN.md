# Kế Hoạch Phase 2 — Tách Trang Chủ / App Riêng, Dashboard, Cài Đặt

## Trạng Thái

Đã chấp nhận (người dùng duyệt ngày 2026-09-26)

## Bối Cảnh

MVP 4 tuần đã hoàn thành: một trang dọc duy nhất chứa mọi chức năng, chưa có
đăng nhập, chưa thể public. Người dùng (là **người thuê phòng**, tự quản chi phí
phòng của mình) sẽ mua domain và chạy production tại nhà, cần:

1. UX/UI chuyên nghiệp, dễ dùng trên điện thoại — thay trang dọc dài bằng điều hướng.
2. Trang chủ portfolio mô tả dự án kèm dữ liệu mẫu.
3. Khi public: khách chỉ thấy trang chủ; app riêng tư nằm sau đăng nhập.
4. Dashboard theo dõi biến động chi phí.
5. Trang Cài đặt gom toàn bộ cấu hình.
6. Icon + animation (landing càng đẹp càng tốt; app dùng tinh tế).
7. CI/CD.
8. README cập nhật lại sau khi xong hết.

## Quyết Định

### Kiến trúc hai vùng

- `/` công khai: portfolio song ngữ (VI/EN, nút chuyển ngôn ngữ), dữ liệu mẫu
  hardcode trong frontend, không gọi API.
- `/app/*` riêng tư: Dashboard · Chỉ số & Hóa đơn · Thanh toán · Cài đặt.
- Toàn bộ `/api/**` (trừ `/api/auth/**` và `/actuator/health`) yêu cầu session.

### Đăng nhập: SĐT + OTP qua Telegram, dự phòng TOTP

- Nhập SĐT → so khớp `OWNER_PHONE` (env) → OTP 6 số gửi qua Telegram bot,
  hiệu lực 5 phút. Rate-limit: 3 lần gửi/15 phút, 5 lần nhập sai/OTP.
- Dự phòng: TOTP authenticator (Google Authenticator), bật trong Settings.
- Session JDBC (sống qua restart), cookie HttpOnly SameSite=Lax, CSRF tắt
  (chấp nhận rủi ro cho app một người, cookie SameSite chặn cross-site POST).
- Tài khoản chủ nhà seed từ env khi khởi động lần đầu; không có trang đăng ký.

### Dashboard (góc nhìn người thuê)

- Thay 4 KPI cũ bằng: **tổng số kỳ + tổng tiền đã bỏ ra thuê từ trước đến giờ**
  (tách đã trả/chưa trả), **kỳ tiêu thụ điện cao nhất**, **kỳ tiêu thụ điện
  thấp nhất**.
- Giữ biểu đồ: cột chi 12 tháng, đường tiêu thụ điện, thành phần chi phí;
  danh sách hóa đơn sắp đến hạn.
- Thư viện: **Recharts** (dependency mới duy nhất cho chart).
- Endpoint tổng hợp: `GET /api/dashboard/summary`.

### Điều hướng

- Mobile: **bottom navigation** 4 mục; Desktop: sidebar.
- React Router; mỗi trang skeleton/empty state riêng; FAB cho hành động chính.

### Landing

- **Framer Motion** (hero, scroll-reveal, counter) + **lucide-react** (icon).
- i18n nhẹ: dictionary + context + localStorage, không thêm thư viện i18n nặng.
- Copy toàn bộ theo góc nhìn **người thuê phòng**.

### CI/CD

- GitHub Actions: push/PR → backend `mvnw verify` + frontend lint/test/build
  + build Docker image; merge main → push ảnh lên **GHCR**.
- Máy nhà deploy bằng `docker compose pull && up -d`; badge CI trên README.

### Production tại nhà

- **Cloudflare Tunnel** + domain: không cần mở port, không lo IP động/CGNAT;
  HTTPS tự động. Guide từng bước trong Task 18 kèm backup volume.

### Sửa hợp đồng

- `PATCH /api/contracts/{id}`: đổi giá áp dụng cho kỳ sau, snapshot cũ bất biến.
- Gia hạn: tạo hợp đồng mới khi hợp đồng cũ `EXPIRED`/`TERMINATED`.

## Phạm Vi Task

| # | Task | Quy mô |
| --- | --- | --- |
| 11 | Auth backend: SĐT + OTP Telegram, session JDBC, TOTP, rate-limit | L |
| 12 | Auth frontend: login 2 bước, guard, interceptor 401 | M |
| 13 | App shell: Router, bottom nav/sidebar, dồn section vào 3 trang | M |
| 14 | Landing song ngữ + Framer Motion + sample data | L |
| 15 | Dashboard: endpoint tổng hợp + stat mới + Recharts | M |
| 16 | Settings: PATCH/gia hạn hợp đồng, Telegram, nhắc, TOTP/tài khoản | M |
| 17 | lucide icons + animation trong app + UX polish | S |
| 18 | Production guide: Cloudflare Tunnel + backup + security review | S |
| 19 | CI/CD: GitHub Actions + GHCR + badge | M |
| 20 | README rewrite Phase 2 | S |

## Hệ Quả

- App UI bên trong giữ tiếng Việt 100%; chỉ landing song ngữ.
- E2E qua API từ Phase 2 trở đi phải đăng nhập trước (session cookie).
- JWT không dùng; CSRF tắt có chủ ý, bù bằng SameSite=Lax + login rate-limit.

# Kế Hoạch Triển Khai MVP Trong 4 Tuần

## Tổng Quan

Xây dựng ứng dụng web responsive cho một người thuê trọ: tạo hợp đồng, tạo hóa đơn từ chỉ số điện cùng tiền nước/phí dịch vụ cố định, xác nhận thanh toán kèm ảnh chứng từ và nhận nhắc Telegram. Kế hoạch ưu tiên một luồng dùng được sớm, sau đó tăng độ tin cậy và hoàn thiện giao diện.

## Tài Liệu Nguồn

- `CAPABILITY-MAP.md`
- `docs/specs/SPEC-billing.md`
- `docs/specs/SPEC-payments.md`
- `docs/specs/SPEC-reminders.md`
- `docs/decisions/ADR-001-kien-truc-mvp.md`

## Sơ Đồ Phụ Thuộc

```text
Khởi tạo dự án + migration
          |
          v
Hợp đồng -> Chỉ số điện -> Hóa đơn
                                  |
                                  v
                   Thanh toán + Chứng từ
                                  |
                                  v
                         Nhắc Telegram
```

## Quyết Định Kiến Trúc

- Modular monolith: `billing`, `payments`, `reminders`; chỉ giao tiếp qua service và REST API, không qua bảng dữ liệu nội bộ của nhau nếu không cần.
- Giá trị tiền dùng VND nguyên hoặc `BigDecimal`; mọi hóa đơn snapshot dữ liệu tính tiền.
- Hạn thanh toán cho phép trễ tối đa ba ngày; scheduler hủy hợp đồng ở ngày trễ thứ tư nếu hóa đơn chưa thanh toán.
- Docker Compose chạy ba service `database`, `backend`, `frontend`; PostgreSQL và chứng từ dùng named volume để giữ dữ liệu qua lần restart container.
- REST API một phiên bản, JSON camelCase, lỗi theo một định dạng chung.
- React ưu tiên mobile; bố cục desktop được mở rộng từ cùng component, không làm hai giao diện riêng.
- Chứng từ và token là tài sản nhạy cảm: file có allowlisted root, token chỉ qua biến môi trường.

## Lệnh Dự Kiến

Các lệnh được xác nhận sau khi khởi tạo dự án ở Tuần 1:

```powershell
./mvnw test -f backend/pom.xml
./mvnw verify -f backend/pom.xml
npm --prefix frontend run test
npm --prefix frontend run build
npm --prefix frontend run lint
docker compose up --build
docker compose down
```

## Lịch 4 Tuần

### Tuần 1 — Nền Tảng Và Luồng Hóa Đơn

- Khởi tạo backend/frontend, Docker Compose, PostgreSQL local, migration và cấu hình không chứa bí mật.
- Hoàn thành hợp đồng, chỉ số điện, công thức tính tiền nước/phí dịch vụ cố định và trang tạo hóa đơn.
- Kiểm chứng sớm migration và công thức tiền bằng unit/integration test.

**Mốc:** Có thể tạo hợp đồng, nhập chỉ số, xem chính xác tổng tiền trên điện thoại.

### Tuần 2 — Thanh Toán Và Chứng Từ

- Hoàn thành xác nhận thanh toán, trạng thái đúng hạn/trễ hạn và lịch sử.
- Hoàn thành upload/stream ảnh chứng từ với giới hạn loại tệp/kích thước và kiểm tra đường dẫn.
- Hoàn thiện trang lịch sử thanh toán mobile-first.

**Mốc:** Có thể dùng app cho một kỳ thực tế và xem lại ảnh chuyển khoản.

### Tuần 3 — Telegram Và Nhắc Hạn

- Hoàn thành cấu hình Telegram qua biến môi trường, tin nhắn thử và adapter HTTP có timeout.
- Hoàn thành scheduler, chống gửi trùng, retry giới hạn và lịch sử nhắc.
- Kiểm thử cảnh báo ngày trễ thứ 1–3 và luồng hủy hợp đồng/ngăn thanh toán ở ngày trễ thứ 4.
- Kiểm chứng end-to-end với chat Telegram thật trước khi thêm giao diện tinh chỉnh.

**Mốc:** Nhận đúng một tin nhắc thử và job không gửi lại cùng loại nhắc trong ngày.

### Tuần 4 — Hoàn Thiện, Kiểm Thử Và Trình Bày CV

- Hoàn thiện trải nghiệm responsive, trạng thái trống/lỗi/tải và accessibility cơ bản.
- Hoàn thành E2E cho luồng chính, kiểm tra bảo mật upload/configuration, build production.
- Viết README, ảnh chụp màn hình và phần mô tả dự án cho CV.

**Mốc:** Một luồng hoàn chỉnh chạy được trên điện thoại, tài liệu hướng dẫn đủ để cài lại từ đầu.

## Rủi Ro Và Giảm Thiểu

| Rủi ro | Mức độ | Giảm thiểu |
| --- | --- | --- |
| Tích hợp Telegram thất bại sát hạn | Cao | Làm tin nhắn thử trong Tuần 1 hoặc đầu Tuần 3, giữ adapter tách biệt để mock. |
| Công thức, snapshot giá hoặc ngưỡng hủy sai | Cao | Unit test bảng trường hợp biên trước khi nối UI. |
| Upload chứng từ gây lỗi bảo mật | Cao | Allowlist ảnh, giới hạn 5 MB, kiểm tra chữ ký tệp và root directory. |
| Scope creep sang quản lý chi tiêu | Trung bình | Mọi hạng mục Phase 2/3 giữ trong backlog, không thêm vào task MVP. |
| Thiếu thời gian giao diện | Trung bình | Hoàn thành mobile-first cho luồng chính trước, chỉ tinh chỉnh sau khi E2E chạy. |
| Khác biệt môi trường máy cá nhân | Trung bình | Docker Compose là cách chạy chuẩn; tài liệu hóa port, volume và biến môi trường. |

## Điểm Kiểm Tra

- Sau Tuần 1: test backend/frontend chạy; công thức hóa đơn được kiểm thử và demo được.
- Sau Tuần 2: luồng hóa đơn → thanh toán → chứng từ chạy end-to-end; review dữ liệu lưu trữ.
- Sau Tuần 3: Telegram gửi thật, scheduler idempotent và không lộ token trong log.
- Sau Tuần 4: build production, tất cả test, checklist MVP và demo mobile hoàn thành.

## Không Làm Trong 4 Tuần

- Đăng nhập, nhiều người dùng/phòng, OCR, chia tiền.
- Dashboard tài chính, ngân sách, category chi tiêu và phân tích điện theo thiết bị.
- Cloud storage, email, queue ngoài hoặc triển khai production công khai.
- Docker Swarm, Kubernetes hoặc triển khai cloud tự động.

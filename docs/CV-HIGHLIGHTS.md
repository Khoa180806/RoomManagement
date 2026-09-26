# Tài Liệu CV — Điểm Nhấn Dự Án Quản Lý Phòng Trọ

Tài liệu này chứa nội dung sẵn để đưa vào CV, LinkedIn và các ý trình bày khi
phỏng vấn. Chọn 3–5 gạch đầu dòng phù hợp với vị trí ứng tuyển; phần "kể chuyện
phỏng vấn" giúp trả lời sâu khi được hỏi.

## 1. Gạch đầu dòng gợi ý cho CV

Chọn theo vị trí:

**Full-stack / Backend (Java):**

- Thiết kế và phát triển hệ thống quản lý phòng trọ end-to-end (Spring Boot 4,
  PostgreSQL 16, React 19, Docker Compose) theo mô hình modular monolith với
  ranh giới module một chiều `billing → payments → reminders`.
- Xây dựng business logic tiền bạc dùng số nguyên VND (không dùng số thực) và
  hóa đơn snapshot: mọi hóa đơn lưu bản sao giá/chỉ số tại thời điểm phát hành,
  đảm bảo dữ liệu quá khứ không bao giờ bị tính lại.
- Cài đặt thanh toán idempotent bằng header `Idempotency-Key` kết hợp hash
  SHA-256 nội dung yêu cầu: cùng key trùng nội dung trả lại kết quả cũ, cùng key
  khác nội dung trả `422`, chống double-submit ở mức API và database constraint.
- Triển khai scheduler nhắc hạn hằng ngày (cron 09:00, timezone
  `Asia/Ho_Chi_Minh`) với dedupe theo khóa (loại nhắc, đối tượng, ngày dự kiến
  gửi, kênh) ngay ở unique constraint database — restart hay chạy lại job không
  bao giờ gửi trùng; retry tối đa 3 lần với backoff 2 ngày trong cửa sổ 7 ngày.
- Tự động hóa nghiệp vụ hủy hợp đồng: job đánh giá độ trễ theo ngày lịch, chuyển
  trạng thái ở ngày trễ thứ tư, chặn thanh toán/hóa đơn/chỉ số mới và đảm bảo
  gửi đúng một thông báo xác nhận qua Telegram.
- Bảo mật upload chứng từ: kiểm tra magic bytes (JPEG/PNG/WebP) thay vì tin MIME
  client, giới hạn 5 MB, tên file UUID do server sinh, chặn path traversal bằng
  normalize + so sánh đường dẫn gốc, file nằm trong volume riêng và stream qua
  API định danh — không bao giờ trả đường dẫn hệ thống.

**Frontend (React/TypeScript):**

- Xây dựng giao diện mobile-first bằng React 19 + TypeScript strict + Tailwind
  CSS 4 (CSS-first tokens), tách lớp `app / components / features / shared` với
  hook điều phối dữ liệu dùng chung.
- Viết bộ test Vitest + Testing Library (21 test) cho helper nghiệp vụ, component
  form/select và accessibility; bổ sung Vitest vào dự án chưa có test runner.
- Xử lý lỗi API có cấu trúc (`error.code/message/details`) thống nhất từ backend
  đến UI với thông báo tiếng Việt, và giữ form state dạng chuỗi cho đến biên API.

**DevOps / Chất lượng:**

- Đóng gói toàn hệ thống bằng Docker Compose (PostgreSQL 16 + Spring Boot +
  Nginx), healthcheck, named volume cho database và chứng từ, biến môi trường
  cho toàn bộ bí mật.
- Duy trì 100+ backend test (JUnit 5, Mockito, H2 ở chế độ PostgreSQL) và
  pipeline kiểm chứng 4 lớp: lint → unit/component → production build → smoke
  test trên Docker.

## 2. Kể chuyện phỏng vấn (STAR rút gọn)

### Hóa đơn snapshot — "tại sao không tính lại từ cấu hình?"

- **Tình huống:** chủ trọ có thể đổi giá điện giữa chừng; nếu hóa đơn cũ tính lại
  theo cấu hình mới thì lịch sử sai.
- **Việc làm:** hóa đơn là entity bất biến chứa bản sao đơn giá, chỉ số cũ/mới,
  tiêu thụ và tổng tiền tại lúc phát hành; hợp đồng chỉ phục vụ kỳ tương lai.
- **Kết quả:** sửa giá hợp đồng không thể ảnh hưởng dữ liệu đã phát hành; unit
  test chốt hành vi "snapshot không đổi" như một hợp đồng kiểm thử.

### Idempotency thanh toán — "chống double-submit ở cấp tiền bạc"

- **Tình huống:** mạng chậm khiến người dùng bấm xác nhận hai lần, hoặc client
  retry sau timeout — không được tạo hai bản ghi thanh toán.
- **Việc làm:** client gửi `Idempotency-Key` (UUID sinh một lần cho mỗi lần bấm,
  giữ nguyên khi retry); server lưu key kèm hash SHA-256 của (billId, paidAt,
  note). Trùng key + trùng hash → trả lại kết quả cũ; trùng key khác hash → 422
  báo xung đột; request song song cùng key bị khóa và unique constraint chặn ở
  database.
- **Kết quả:** xử lý đúng cả ba luồng lỗi/thành công, được kiểm chứng cả bằng
  unit test lẫn E2E qua HTTP thật.

### Scheduler nhắc hạn — "idempotent theo ngày và chống gửi trùng"

- **Tình huống:** job chạy lúc 09:00 hằng ngày; nếu restart giữa chừng hoặc chạy
  lại trong ngày, không được gửi tin trùng cho người dùng.
- **Việc làm:** mỗi reminder định danh bởi (loại nhắc, đối tượng, ngày dự kiến
  gửi, kênh) và được unique constraint chặn ở database; trạng thái SENT/FAILED
  đọc từ database chứ không giữ trong bộ nhớ; gửi lỗi retry tối đa 3 lần với
  backoff 2 ngày trong cửa sổ 7 ngày; mỗi lần gửi nằm trong transaction riêng
  để một bill lỗi không ảnh hưởng phần còn lại của ngày.
- **Kết quả:** xác minh trực tiếp trên Docker: job chạy lại nhiều lần trong ngày
  chỉ tạo đúng một bản ghi SENT; hóa đơn đã trả không bao giờ tạo nhắc quá hạn.

### Telegram — "bí mật và độ tin cậy"

- **Tình huống:** bot token là bí mật; lỗi mạng/HTTP không được làm sập job và
  không được lộ token qua log.
- **Việc làm:** token/chat ID chỉ nằm trong biến môi trường, được kiểm tra khi
  khởi động (cảnh báo không log giá trị); adapter Telegram nằm sau interface để
  mock trong test; mọi exception HTTP client bị chặn và thay bằng thông báo lỗi
  an toàn kèm mã lỗi để retry; tin nhắn chỉ chứa kỳ, hạn, tổng tiền và trạng
  thái — không có dữ liệu nhạy cảm.
- **Kết quả:** kiểm thử adapter bằng mock HTTP server, assert token chỉ xuất
  hiện trong URL gọi ra và không bao giờ trong exception.

### Bảo mật upload — "không tin gì từ client"

- **Tình huống:** ảnh chứng từ là điểm vào phổ biến cho payload độc.
- **Việc làm:** kiểm tra magic bytes thay vì MIME/extension do client khai báo;
  giới hạn 5 MB ở cả Spring multipart lẫn validator nghiệp vụ; đổi tên file
  UUID do server sinh; resolve + normalize đường dẫn và từ chối mọi đường dẫn
  thoát thư mục gốc; stream file từ volume riêng qua API định danh, không bao
  giờ trả đường dẫn hệ thống trong response.
- **Kết quả:** bộ integration test phủ từng lớp: PDF giả MIME, file rỗng, quá
  kích thước (413/422), chặn trùng chứng từ, và path traversal.

## 3. Con số đáng nhắc

- 100 backend test (JUnit 5 + Mockito + Flyway/H2 PostgreSQL mode) và 21
  frontend test (Vitest + Testing Library) — toàn bộ xanh.
- 8 Flyway migration, schema có CHECK constraint cho tiền/chỉ số/trạng thái.
- E2E end-to-end đã chạy thật: hợp đồng → chỉ số → hóa đơn → thanh toán
  (idempotent) → chứng từ → nhắc thử Telegram.

## 4. Ghi chú khi đưa vào CV

- Ưu tiên 3–5 gạch đầu dòng liên quan vị trí; không liệt kê hết.
- Đi kèm link GitHub và 1 câu mô tả giá trị ("tự động hóa toàn bộ chu trình
  thu tiền phòng trọ, từ chỉ số điện đến nhắc hạn Telegram").
- Sẵn sàng vẽ lại sơ đồ kiến trúc (README có sẵn mermaid) khi phỏng vấn.

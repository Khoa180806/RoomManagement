# Đặc Tả `reminders`: Nhắc Hạn Telegram

## Mục Tiêu

Tự động gửi thông báo Telegram để người dùng không bỏ lỡ hạn đóng tiền hoặc ngày hết hạn hợp đồng, đồng thời không gửi lặp vô ích.

## Phụ Thuộc

`reminders` đọc hóa đơn và hợp đồng từ `billing`, đọc trạng thái thanh toán từ `payments`. Nó không tự sửa hóa đơn hoặc thanh toán: việc chuyển hóa đơn `PENDING` sang `OVERDUE` là capability của `billing` mà job hằng ngày của `reminders` kích hoạt.

## Môi Trường Thực Thi Và Cấu Trúc

- Dùng Java 21, Spring Boot 3.x và PostgreSQL 16; Spring Scheduler chạy job, React 19 + TypeScript + Vite hiển thị cài đặt/lịch sử.
- Mã backend nằm trong `backend/src/main/java/.../reminders/`; test nằm trong `backend/src/test/java/.../reminders/`.
- Giao diện nằm trong `frontend/src/features/reminders/` và `frontend/src/features/settings/`.
- Lệnh kiểm chứng: `./mvnw test -f backend/pom.xml`, `npm --prefix frontend run test`, `npm --prefix frontend run build`.

## Phong Cách Mã Nguồn

- Giao tiếp Telegram nằm sau interface riêng để test bằng mock; scheduler gọi service thay vì gọi HTTP trực tiếp.
- API dùng JSON camelCase, enum `UPPER_SNAKE_CASE` và định dạng lỗi chung đã định nghĩa trong `billing`.
- Thông tin token/chat ID không được đặt trong DTO, entity response hoặc log.

## Quy Tắc Nhắc

- Cấu hình mặc định cho hạn thanh toán: trước hạn 7, 3 và 1 ngày; nhắc quá hạn ở ngày trễ thứ 1, 2 và 3, nêu rõ rằng hợp đồng sẽ bị hủy nếu sang ngày trễ thứ 4 vẫn chưa thanh toán.
- Ở ngày trễ thứ 4, job hủy hợp đồng theo `billing` rồi gửi một thông báo Telegram xác nhận hợp đồng đã bị hủy do không thanh toán.
- Cấu hình mặc định cho hợp đồng: trước ngày kết thúc 60 và 30 ngày.
- Người dùng có thể bật/tắt từng loại nhắc và thay đổi số ngày trong giao diện cài đặt.
- Scheduler chạy mỗi ngày lúc 09:00 theo `Asia/Ho_Chi_Minh`. Các job phải idempotent theo khóa gồm loại nhắc, đối tượng, ngày dự kiến gửi và kênh.
- Mỗi lần gửi, lưu `Reminder` với trạng thái `SENT` hoặc `FAILED`, thời điểm gửi và mã lỗi an toàn để retry. Không lưu bot token hay toàn bộ nội dung lỗi Telegram.
- Thông điệp thanh toán gồm kỳ, hạn, tổng tiền và trạng thái; thông điệp hợp đồng gồm ngày hết hạn. Không gửi ảnh chứng từ hay dữ liệu không cần thiết.

## Hợp Đồng API

| Phương thức | Đường dẫn | Mục đích |
| --- | --- | --- |
| `GET` | `/api/reminder-settings` | Lấy cấu hình nhắc hiện hành. |
| `PUT` | `/api/reminder-settings` | Cập nhật ngày nhắc và trạng thái bật/tắt. |
| `GET` | `/api/reminders?page=0&pageSize=20` | Xem lịch sử gửi, phân trang (page tính từ 0). |
| `POST` | `/api/reminders/test` | Gửi một tin nhắn thử đến chat ID đã cấu hình. |

`POST /api/reminders/test` chỉ được dùng để kiểm chứng cấu hình lúc cài đặt. Trong MVP, chat ID lấy từ biến môi trường phía server nên endpoint không nhận body; client không thể ghi hoặc đọc bot token/chat ID.

## Bảo Mật Và Độ Tin Cậy

- Bot token và chat ID lấy từ biến môi trường; ứng dụng kiểm tra khi khởi động và ghi cảnh báo nếu chưa cấu hình hoặc token sai định dạng (không bao giờ log giá trị).
- Chỉ gọi Telegram qua HTTPS; timeout hữu hạn. Phản hồi Telegram là dữ liệu không tin cậy và được kiểm tra trước khi dùng.
- Lỗi một lần gửi không được làm hỏng scheduler; retry giới hạn, có backoff và ghi trạng thái `FAILED`. Mỗi lần gửi nằm trong transaction riêng; exception của một bill không làm bỏ qua phần còn lại của ngày.
- Scheduler truy vấn theo thời gian và trạng thái ở database, không dựa vào bộ nhớ; sau khi restart không gửi trùng. Reminder `FAILED` được retry trong cửa sổ 7 ngày kể từ ngày dự kiến gửi, backoff 2 ngày giữa các lần thử.
- Cron có thể ghi đè qua biến `APP_REMINDERS_CRON` chỉ để phục vụ kiểm thử local; mặc định không đổi tần suất nghiệp vụ.

## Kiểm Thử

- Unit test xác định đúng ngày đến hạn, ngày nhắc và loại nhắc cần gửi.
- Integration test unique constraint chống gửi trùng và xử lý retry.
- Test adapter Telegram bằng mock HTTP server; xác nhận request có token chỉ trong URL gọi ra và không xuất hiện trong log/assertion.
- E2E chạy job với dữ liệu thời gian cố định và xem lịch sử `SENT` trên giao diện.

## Ranh Giới

- **Luôn làm:** job idempotent, timeout mạng, giữ bí mật token và dùng timezone cố định.
- **Hỏi trước:** thêm email, thay đổi timezone, thay đổi tần suất nhắc quá hạn hoặc dùng queue bên ngoài.
- **Không làm:** gửi tin nhắn cho nhiều người, gửi thông báo sau khi hóa đơn đã trả, gọi Telegram trực tiếp từ React.

## Tiêu Chí Hoàn Thành

- Tin nhắn thử đến đúng chat Telegram đã cấu hình.
- Đúng một tin được gửi cho mỗi loại nhắc trong một ngày hợp lệ, kể cả khi job chạy lại.
- Hóa đơn đã trả không còn tạo nhắc quá hạn; hợp đồng hủy do trễ quá ba ngày tạo đúng một thông báo xác nhận và lịch sử gửi hiển thị được trạng thái thành công/thất bại.

## Câu Hỏi Mở

- Chat ID sẽ được cấu hình một lần bằng biến môi trường cho MVP; nếu cần đổi qua giao diện thì phải thêm cơ chế bảo vệ cấu hình nhạy cảm.

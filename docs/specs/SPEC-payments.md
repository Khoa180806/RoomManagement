# Đặc Tả `payments`: Thanh Toán Và Chứng Từ

## Mục Tiêu

Cho phép người dùng xác nhận một hóa đơn đã thanh toán, tải ảnh chuyển khoản và đối chiếu lịch sử theo trạng thái đúng hạn hoặc trễ hạn.

## Phụ Thuộc

Khả năng này chỉ thao tác trên hóa đơn do `billing` tạo. Nó không thay đổi số tiền, kỳ hóa đơn hay snapshot đơn giá.

## Môi Trường Thực Thi Và Cấu Trúc

- Dùng Java 21, Spring Boot 3.x, PostgreSQL 16, React 19, TypeScript và Vite như `billing`.
- Mã backend nằm trong `backend/src/main/java/.../payments/`; test nằm trong `backend/src/test/java/.../payments/`.
- Giao diện nằm trong `frontend/src/features/payments/` và `frontend/src/features/receipts/`.
- Lệnh kiểm chứng: `./mvnw test -f backend/pom.xml`, `npm --prefix frontend run test`, `npm --prefix frontend run build`.

## Phong Cách Mã Nguồn

- Tách request/response DTO khỏi entity JPA; API dùng JSON camelCase và enum `UPPER_SNAKE_CASE`.
- Mọi lỗi tuân theo định dạng `error.code`, `error.message`, `error.details` của `billing`.
- Không dùng `double` cho tiền; React dùng component hàm TypeScript và tự động escape nội dung hiển thị.

## Nghiệp Vụ Và Dữ Liệu

- Một hóa đơn chỉ có một bản ghi thanh toán trong MVP.
- Khi xác nhận, người dùng gửi `paidAt` và có thể gửi ghi chú. `paidAt` không được nằm trong tương lai.
- `onTime` là `true` khi `paidAt` không muộn hơn hạn thanh toán; ngược lại là `false`.
- Hóa đơn của hợp đồng `TERMINATED_FOR_NON_PAYMENT` không thể được xác nhận thanh toán trong MVP; API trả `409 CONTRACT_TERMINATED`.
- Thanh toán được lưu với trạng thái `PAID`; không cho phép xác nhận lần hai (`409`). Nếu cần sửa, phải có thao tác hoàn tác được thiết kế riêng sau MVP.
- Chứng từ là ảnh JPEG, PNG hoặc WebP, tối đa 5 MB. Backend kiểm tra kích thước, MIME type và chữ ký tệp; không tin phần mở rộng do client gửi.
- Tệp được đổi tên bằng UUID, lưu trong thư mục gốc cấu hình qua biến môi trường. API chỉ trả URL tải có định danh chứng từ, không trả đường dẫn hệ thống tệp.

## Hợp Đồng API

| Phương thức | Đường dẫn | Mục đích |
| --- | --- | --- |
| `POST` | `/api/bills/{id}/payments` | Xác nhận đã thanh toán một hóa đơn `PENDING` hoặc `OVERDUE`. |
| `GET` | `/api/payments?page=1&pageSize=12` | Xem lịch sử, phân trang và lọc `onTime`. |
| `GET` | `/api/payments/{id}` | Xem chi tiết thanh toán cùng thông tin hóa đơn. |
| `POST` | `/api/payments/{id}/receipts` | Tải một ảnh chứng từ dạng `multipart/form-data`. |
| `GET` | `/api/receipts/{id}` | Stream ảnh chứng từ với `Content-Type` đã kiểm tra. |

Tạo thanh toán phải idempotent: client gửi `Idempotency-Key` ổn định cho một lần bấm xác nhận; backend lưu key cùng hash nội dung trong unique constraint. Cùng key nhưng khác nội dung trả `422`; yêu cầu trùng đang xử lý trả `409`.

## Bảo Mật

- `TELEGRAM_BOT_TOKEN`, cấu hình database và thư mục chứng từ chỉ nằm trong biến môi trường; `.env` không được commit.
- Không ghi log ảnh, nội dung chứng từ, token hoặc đường dẫn tuyệt đối.
- Khi đọc/xóa tệp, resolve đường dẫn rồi kiểm tra nó nằm dưới thư mục chứng từ cho phép; không nhận đường dẫn tệp từ request.
- Trả lỗi chung cho tệp không hợp lệ; không để lộ stack trace hay cấu trúc thư mục.

## Kiểm Thử

- Unit test phân loại đúng hạn/trễ hạn ở cả hai biên ngày đến hạn.
- Integration test unique constraint cho thanh toán/idempotency và phản hồi lỗi thống nhất.
- Integration test upload: chấp nhận ảnh hợp lệ, từ chối MIME giả, chữ ký sai, tệp vượt 5 MB và đường dẫn thoát thư mục.
- E2E tạo hóa đơn → xác nhận thanh toán → tải chứng từ → mở lại chứng từ trên giao diện điện thoại.

## Ranh Giới

- **Luôn làm:** kiểm tra tệp tại server, dùng tên tệp do server tạo, parameterized query/ORM và không trả đường dẫn local.
- **Hỏi trước:** thêm cloud storage, OCR, xóa chứng từ hoặc sửa thanh toán đã xác nhận.
- **Không làm:** lưu ảnh dạng BLOB trong database, chấp nhận PDF/HEIC trong MVP, dùng client-side validation như biên bảo mật.

## Tiêu Chí Hoàn Thành

- Một hóa đơn có thể được đánh dấu đã trả đúng một lần, với trạng thái đúng hạn/trễ hạn chính xác.
- Thanh toán trễ tối đa ba ngày vẫn được ghi nhận; từ ngày trễ thứ tư, trạng thái hủy hợp đồng chặn thanh toán mới.
- Ảnh chứng từ hợp lệ tải lên và xem lại được; tệp không hợp lệ không tồn tại trên ổ đĩa sau khi request thất bại.
- Lịch sử cho biết kỳ, số tiền, thời điểm trả, trạng thái và có chứng từ hay không.

## Câu Hỏi Mở

- MVP chỉ hỗ trợ một ảnh chứng từ cho mỗi thanh toán; nếu cần nhiều ảnh, phải thiết kế lại quan hệ dữ liệu ở Phase 2.

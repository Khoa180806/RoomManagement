# Bản Đồ Khả Năng: MVP Quản Lý Phòng Trọ

## Mục Đích

MVP được chia thành các khả năng có thể xây dựng và kiểm thử độc lập. Thứ tự dưới đây giúp hoàn thành luồng quan trọng nhất trước, đồng thời giảm rủi ro cho kế hoạch bốn tuần.

## Các Khả Năng

| Mã khả năng | Trách nhiệm | Phụ thuộc |
| --- | --- | --- |
| `billing` | Quản lý hợp đồng, cấu hình các khoản cố định, ghi chỉ số điện và tạo hóa đơn theo kỳ. | Không có |
| `payments` | Xem khoản cần thanh toán, xác nhận đã trả, tải ảnh chứng từ và phân loại đúng hạn/trễ hạn. | `billing` |
| `reminders` | Tạo và gửi thông báo Telegram cho hạn thanh toán, quá hạn và ngày hết hạn hợp đồng. | `billing`, `payments` |

## Thứ Tự Xây Dựng

`billing` → `payments` → `reminders`

## Giả Định Ban Đầu

- Ứng dụng là web app responsive, dùng trên điện thoại qua trình duyệt.
- MVP phục vụ một người dùng và một hợp đồng thuê đang hiệu lực; chưa cần đăng nhập hay phân quyền.
- Backend dùng Spring Boot, Spring Data JPA, Spring Scheduler và PostgreSQL.
- Frontend dùng React.
- Chứng từ được lưu trong local storage có thể cấu hình ở backend; không lưu trong cơ sở dữ liệu.
- Một hóa đơn phải lưu bản sao tiền phòng, đơn giá điện, tiền nước cố định và phí dịch vụ cố định tại lúc tạo hóa đơn.

## Ngoài Phạm Vi

- Nhiều người dùng, nhiều phòng, chia tiền hoặc vai trò chủ trọ.
- OCR chứng từ, quản lý thu chi tổng quát và tối ưu điện năng.

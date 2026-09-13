# ADR-003: Docker Compose Và Commit Theo Thay Đổi Logic

## Trạng Thái

Đã chấp nhận

## Ngày

2026-09-13

## Bối Cảnh

MVP dùng nhiều thành phần chạy cùng nhau: PostgreSQL, Spring Boot, React và local storage cho chứng từ. Chạy thủ công dễ tạo khác biệt cấu hình giữa các máy. Dự án cũng cần lịch sử Git dễ đối chiếu khi làm portfolio/CV.

## Quyết Định

- Docker Compose là cách chạy local chuẩn, gồm các service `database`, `backend` và `frontend`.
- Dữ liệu PostgreSQL và chứng từ dùng named volume; biến môi trường nằm trong `.env` local, còn `.env.example` chỉ có placeholder.
- Mỗi thay đổi logic đã kiểm thử tạo một commit độc lập với thông điệp có ý nghĩa theo Conventional Commits, ví dụ `feat: tạo hóa đơn từ chỉ số điện`.
- Không gom nhiều thay đổi không liên quan vào một commit; cũng không tách một thay đổi logic thành commit giả tạo chỉ để tăng số lượng.

## Hệ Quả

- Dockerfile, `compose.yaml` và hướng dẫn chạy trở thành một phần của Task 1.
- Trước mỗi commit phải xem diff staged, kiểm tra bí mật và chạy các kiểm thử phù hợp với thay đổi.
- Lịch sử Git sẽ có nhiều mốc nhỏ, nhưng mỗi mốc phải build/test được trong phạm vi thay đổi.

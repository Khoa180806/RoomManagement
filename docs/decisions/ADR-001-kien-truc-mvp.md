# ADR-001: Kiến Trúc MVP Một Người Dùng

## Trạng Thái

Đề xuất

## Ngày

2026-09-12

## Bối Cảnh

MVP cần dùng thật trong một tháng, nhưng vẫn phải thể hiện mô hình dữ liệu, business logic, scheduler, upload tệp và tích hợp Telegram. Chỉ một người thuê sử dụng nên phân quyền nhiều người không tạo ra giá trị tương xứng.

## Quyết Định

- Backend là modular monolith dùng Spring Boot, Spring Data JPA, Spring Scheduler và PostgreSQL.
- Frontend là React + TypeScript + Vite, giao tiếp với REST API cùng một phiên bản.
- Không xây đăng nhập trong MVP; ứng dụng chỉ chạy cho người dùng cục bộ. Trước khi public lên Internet, phải bổ sung xác thực và phân quyền.
- Ảnh chứng từ nằm trong thư mục local storage được cấu hình bằng biến môi trường; database chỉ lưu metadata và định danh tệp.
- Telegram bot token và chat ID nằm trong biến môi trường, không nằm trong source code hay database.

## Các Phương Án Đã Cân Nhắc

### MySQL

- Ưu điểm: phổ biến và dễ tìm tài liệu.
- Không chọn: PostgreSQL có kiểu dữ liệu và tính năng tốt cho dữ liệu thời gian, đồng thời đáp ứng đầy đủ MVP.

### Cloud storage cho chứng từ

- Ưu điểm: dễ mở rộng và truy cập đa thiết bị.
- Không chọn cho MVP: thêm tài khoản, chi phí, quyền truy cập và rủi ro cấu hình; local storage phù hợp ứng dụng cá nhân.

### Đăng nhập ngay từ đầu

- Ưu điểm: dễ public sau này.
- Không chọn cho MVP: thêm luồng đăng ký, session, reset mật khẩu và kiểm thử bảo mật; không phục vụ mục tiêu một người dùng trong tháng đầu.

## Hệ Quả

- Cần tài liệu hướng dẫn chạy local và sao lưu thư mục chứng từ cùng database.
- API vẫn phải có validation, kiểm soát upload và bí mật cấu hình như một ứng dụng có thể mở rộng.
- Nếu triển khai công khai hoặc đa người dùng, cần ADR mới về xác thực, phân quyền và lưu trữ tệp trước khi phát hành.

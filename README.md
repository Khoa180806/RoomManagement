# Ứng dụng quản lý phòng trọ

Ứng dụng cá nhân hỗ trợ theo dõi hợp đồng thuê trọ, tiền phòng, tiền điện,
nước và dịch vụ cố định. MVP ưu tiên dùng tốt trên điện thoại và gửi nhắc hạn
qua Telegram.

## Phạm vi MVP

- Lưu một hợp đồng đang hiệu lực, hạn thanh toán và ngày hết hạn hợp đồng.
- Tính tiền điện từ chỉ số cũ/mới; tiền nước và dịch vụ là các khoản cố định.
- Lưu hóa đơn, thanh toán và ảnh chứng từ chuyển khoản.
- Nhắc hạn qua Telegram; quá hạn quá 3 ngày lịch thì hủy hợp đồng.

Tài liệu nghiệp vụ chi tiết nằm trong thư mục `docs/` và kế hoạch thực hiện ở
`tasks/plan.md`.

## Công nghệ

- Backend: Java 21, Spring Boot, Spring Data JPA, Flyway, PostgreSQL.
- Frontend: React, TypeScript, Vite.
- Môi trường chạy: Docker Compose, PostgreSQL 16, Nginx.

## Chạy bằng Docker

1. Cài Docker Desktop và mở Docker Desktop.
2. Sao chép `.env.example` thành `.env`.
3. Đổi `POSTGRES_PASSWORD` trong `.env` thành mật khẩu chỉ dùng trên máy local.
4. Tại thư mục gốc dự án, chạy:

```powershell
docker compose up --build
```

Sau khi các dịch vụ khởi động:

- Giao diện: `http://localhost:8080`
- Health endpoint của API: `http://localhost:8081/actuator/health`

Dừng các container nhưng giữ dữ liệu database và chứng từ:

```powershell
docker compose down
```

Hai Docker volume `postgres-data` và `receipts-data` giúp dữ liệu local không
mất khi container được tạo lại. Không commit tệp `.env` hoặc ảnh chứng từ thật.

## Chạy khi phát triển

### Backend

Tạo PostgreSQL local tên `room_management`, sau đó cấu hình ba biến môi trường
`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` nếu khác giá trị mặc
định. Chạy từ thư mục `backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

Kiểm thử và build backend:

```powershell
.\mvnw.cmd verify
```

### Frontend

Chạy từ thư mục `frontend`:

```powershell
npm ci
npm run dev
```

Kiểm tra chất lượng frontend:

```powershell
npm run lint
npm run test
npm run build
```

Frontend được tổ chức theo `app`, `components`, `features` và `shared`. State/API
cấp workspace nằm ở `frontend/src/app/useRentalWorkspace.ts`; API feature dùng
request wrapper chung tại `frontend/src/shared/api/client.ts`. Tailwind CSS v4
được cấu hình theo CSS-first, token nằm ở `frontend/src/shared/styles/` và
stylesheet `App.css` cũ đã được loại bỏ. Xem [frontend/README.md](frontend/README.md)
để biết quy ước thư mục và test component.

## Lưu ý an toàn

- Đây là ứng dụng một người dùng, chưa có đăng nhập; không đưa lên Internet
  trước khi bổ sung xác thực và phân quyền.
- Telegram bot token và chat ID chỉ được cấu hình qua biến môi trường
  `TELEGRAM_BOT_TOKEN`/`TELEGRAM_CHAT_ID` trong `.env`; không commit, không log
  và không trả qua API. Gửi tin nhắn thử từ mục "Telegram nhắc hạn" trên giao diện.
- Ảnh chứng từ sẽ lưu trên volume local, metadata lưu trong database; không lưu
  ảnh dưới dạng BLOB.

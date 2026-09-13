# Đặc Tả `billing`: Hợp Đồng Và Hóa Đơn

## Mục Tiêu

Cho phép người thuê lưu một hợp đồng đang hiệu lực, cấu hình chi phí cố định và tạo hóa đơn hằng tháng từ chỉ số điện. Đây là nguồn dữ liệu duy nhất cho thanh toán và nhắc hạn.

## Công Nghệ Dự Kiến

- Java 21, Spring Boot 3.x, Spring Data JPA, Bean Validation và Maven Wrapper.
- PostgreSQL 16; Flyway quản lý migration.
- React 19, TypeScript, Vite và React Router.
- Docker Compose chạy PostgreSQL, backend và frontend trong môi trường local nhất quán.

## Cấu Trúc Dự Án Dự Kiến

```text
backend/src/main/java/.../billing/  # Entity, repository, service và REST controller
backend/src/test/java/.../billing/  # Unit test và integration test
frontend/src/features/billing/      # Màn hình hợp đồng, chỉ số và hóa đơn
frontend/src/lib/api/               # API client có kiểu dữ liệu
docs/specs/                         # Đặc tả theo khả năng
tasks/                              # Kế hoạch và checklist thực hiện
```

## Nghiệp Vụ Và Dữ Liệu

### Hợp đồng thuê

- Chỉ có một hợp đồng ở trạng thái `ACTIVE`; tạo hợp đồng mới chỉ được phép sau khi hợp đồng cũ là `EXPIRED` hoặc `TERMINATED`.
- Trường bắt buộc: ngày bắt đầu, ngày kết thúc, ngày đến hạn thanh toán hằng tháng từ 1 đến 28, tiền phòng, đơn giá điện, tiền nước cố định và phí dịch vụ cố định.
- Tiền và đơn giá là số nguyên không âm theo VND. Ngày kết thúc phải sau ngày bắt đầu.
- Tiền nước và phí dịch vụ là khoản cố định trong suốt hợp đồng. Hóa đơn đã tạo không được thay đổi khi cấu hình của hợp đồng sau này được sửa.

### Chỉ số điện

- Mỗi hợp đồng có tối đa một chỉ số điện cho một kỳ `YYYY-MM`.
- Chỉ số mới phải lớn hơn hoặc bằng chỉ số điện gần nhất trước đó.
- Kỳ hóa đơn được tạo khi đã có chỉ số điện. Không cho phép tạo trùng kỳ cho cùng hợp đồng.

### Hóa đơn

- Điện = chỉ số mới - chỉ số cũ.
- Tổng tiền = tiền phòng + điện tiêu thụ × đơn giá điện + tiền nước cố định + phí dịch vụ cố định.
- Hóa đơn lưu bản sao toàn bộ giá trị, chỉ số điện và mức tiêu thụ đã dùng. Không tính lại quá khứ từ cấu hình hiện hành.
- Hạn thanh toán là ngày cấu hình của tháng tương ứng. Nếu ngày đó không tồn tại thì dùng ngày cuối tháng; MVP giới hạn 1–28 để loại bỏ trường hợp này.
- Hóa đơn tạo với trạng thái `PENDING`; chỉ khả năng `payments` mới có thể chuyển sang `PAID` hoặc `OVERDUE`.
- `overdueDays` là số ngày lịch từ ngày sau hạn thanh toán đến ngày hiện tại theo `Asia/Ho_Chi_Minh`. Thanh toán ở ngày trễ thứ 1–3 vẫn hợp lệ; từ ngày trễ thứ 4, hợp đồng chuyển sang `TERMINATED_FOR_NON_PAYMENT`.
- Sau khi bị hủy vì không thanh toán, không tạo thêm hóa đơn hoặc chỉ số điện cho hợp đồng đó. Không được khôi phục tự động; mọi thay đổi trạng thái sau đó yêu cầu nghiệp vụ mới ngoài MVP.

## Hợp Đồng API

Mọi lỗi dùng định dạng:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dữ liệu không hợp lệ",
    "details": []
  }
}
```

| Phương thức | Đường dẫn | Mục đích |
| --- | --- | --- |
| `GET` | `/api/contracts/active` | Lấy hợp đồng đang hiệu lực hoặc `404`. |
| `POST` | `/api/contracts` | Tạo hợp đồng. |
| `PATCH` | `/api/contracts/{id}` | Cập nhật cấu hình áp dụng cho kỳ sau. |
| `POST` | `/api/electricity-readings` | Ghi chỉ số điện cho một kỳ. |
| `GET` | `/api/electricity-readings?period=YYYY-MM` | Xem chỉ số điện của kỳ. |
| `POST` | `/api/bills` | Tạo hóa đơn từ hai chỉ số hợp lệ. |
| `GET` | `/api/bills?period=YYYY-MM` | Danh sách hóa đơn, phân trang mặc định 12 kỳ. |
| `GET` | `/api/bills/{id}` | Xem chi tiết một hóa đơn. |

`POST /api/bills` trả `201`; tạo lại cùng hợp đồng và kỳ trả `409`. Dữ liệu đầu vào được kiểm tra ở controller; service chỉ nhận dữ liệu đã hợp lệ.

## Phong Cách Mã Nguồn

- Java dùng `PascalCase` cho lớp, `camelCase` cho thuộc tính/phương thức và `UPPER_SNAKE_CASE` cho enum.
- Tách request/response DTO khỏi entity JPA; dùng `BigDecimal` hoặc số nguyên VND, không dùng `double` cho tiền.
- React dùng component hàm TypeScript và đặt dữ liệu từ API theo kiểu dữ liệu đã khai báo.

```java
public record CreateElectricityReadingRequest(
    @NotNull YearMonth period,
    @NotNull @PositiveOrZero Long meterValue
) {}
```

## Chiến Lược Kiểm Thử

- Unit test công thức hóa đơn, chênh lệch chỉ số, luật không tạo trùng kỳ và snapshot đơn giá.
- Integration test repository và REST controller với PostgreSQL Testcontainers.
- Frontend test xác thực biểu mẫu và hiển thị tổng tiền; E2E tạo hợp đồng → nhập chỉ số → tạo hóa đơn.

## Ranh Giới

- **Luôn làm:** xác thực ở API boundary, migration có thể lặp lại, kiểm thử công thức tiền và lưu tiền bằng VND nguyên.
- **Hỏi trước:** thay đổi schema đã có migration, thêm thư viện, thay đổi công thức tính, ngưỡng hủy hợp đồng hoặc thêm loại phí.
- **Không làm:** sửa hóa đơn đã phát hành, dùng số thực cho tiền, tạo nhiều hợp đồng `ACTIVE` hoặc khôi phục tự động hợp đồng bị hủy.

## Tiêu Chí Hoàn Thành

- Người dùng tạo được hợp đồng và xem lại cấu hình hiện hành trên điện thoại.
- Với các chỉ số điện cũ/mới, tổng hóa đơn đúng theo công thức và không bị đổi khi giá điện sau này thay đổi.
- Hợp đồng chỉ bị hủy khi hóa đơn chưa trả và đã trễ sang ngày thứ tư theo timezone cấu hình.
- Dữ liệu không hợp lệ, chỉ số giảm và kỳ trùng nhận lỗi có cấu trúc, không tạo dữ liệu dở dang.

## Câu Hỏi Mở

- Tiền phòng hiện được xem là cố định theo hợp đồng; nếu có nhu cầu thay đổi giữa hợp đồng, cần thêm quy tắc hiệu lực theo kỳ.

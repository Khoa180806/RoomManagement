# Task 3: Nhập Chỉ Số Điện Và Tạo Hóa Đơn

## Tổng quan

Thêm 2 entity mới (`ElectricityReading`, `Bill`) vào module `billing`, tuân theo đúng patterns đã có từ Task 2 (factory method, 2-layer DTO, structured error, Flyway migration, unit/controller test). Frontend mở rộng App.tsx thêm form nhập chỉ số và hiển thị hóa đơn.

---

## Bước 1 — Flyway Migration

Tạo `V3__create_electricity_readings_and_bills.sql`:

### Bảng `electricity_readings`
| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| `id` | `UUID` | PRIMARY KEY |
| `contract_id` | `UUID` | NOT NULL, FK → rental_contracts(id) |
| `period` | `VARCHAR(7)` | NOT NULL, định dạng `YYYY-MM` |
| `meter_value` | `BIGINT` | NOT NULL, CHECK >= 0 |
| `recorded_at` | `TIMESTAMPTZ` | NOT NULL |
| **Unique** | `(contract_id, period)` | Chống trùng kỳ |

### Bảng `bills`
| Cột | Kiểu | Ràng buộc |
|-----|------|-----------|
| `id` | `UUID` | PRIMARY KEY |
| `contract_id` | `UUID` | NOT NULL, FK → rental_contracts(id) |
| `period` | `VARCHAR(7)` | NOT NULL |
| `rent_amount` | `BIGINT` | NOT NULL, CHECK >= 0 |
| `electricity_unit_price` | `BIGINT` | NOT NULL, CHECK >= 0 |
| `water_fee` | `BIGINT` | NOT NULL, CHECK >= 0 |
| `service_fee` | `BIGINT` | NOT NULL, CHECK >= 0 |
| `old_meter_value` | `BIGINT` | NOT NULL, CHECK >= 0 |
| `new_meter_value` | `BIGINT` | NOT NULL, CHECK >= 0 |
| `consumption` | `BIGINT` | NOT NULL, CHECK >= 0 |
| `electricity_amount` | `BIGINT` | NOT NULL, CHECK >= 0 |
| `total_amount` | `BIGINT` | NOT NULL, CHECK >= 0 |
| `status` | `VARCHAR(20)` | NOT NULL, CHECK IN ('PENDING','PAID','OVERDUE') |
| `due_date` | `DATE` | NOT NULL |
| `created_at` | `TIMESTAMPTZ` | NOT NULL |
| **CHECK** | `new_meter_value >= old_meter_value` | |
| **CHECK** | `consumption = new_meter_value - old_meter_value` | |
| **CHECK** | `total_amount = rent_amount + electricity_amount + water_fee + service_fee` | |
| **CHECK** | `electricity_amount = consumption * electricity_unit_price` | |
| **Unique** | `(contract_id, period)` | Chống trùng hóa đơn |

---

## Bước 2 — Backend: Electricity Readings

Package: `com.khoa.roommanagement.billing.electricity`

### Entity: `ElectricityReading`
- Factory method `record(contractId, period, meterValue)` → gán UUID, `recordedAt = Instant.now()`
- Fields: `id`, `contractId`, `period` (String `YYYY-MM`), `meterValue`, `recordedAt`

### Repository: `ElectricityReadingRepository`
- `Optional<ElectricityReading> findByContractIdAndPeriod(UUID, String)`
- `List<ElectricityReading> findByContractIdOrderByPeriodDesc(UUID)`
- `boolean existsByContractIdAndPeriod(UUID, String)`

### Service: `ElectricityReadingService`
- `record(CreateReadingCommand)`:
  1. Kiểm tra hợp đồng ACTIVE tồn tại → throw nếu không
  2. Kiểm tra hợp đồng không bị `TERMINATED_FOR_NON_PAYMENT` → throw nếu bị
  3. Kiểm tra chưa có reading cho kỳ → throw `DuplicateReadingException` (409)
  4. Lấy reading gần nhất (kỳ trước) → kiểm tra `meterValue >= previousMeterValue` → throw nếu giảm
  5. Save reading
- `getByPeriod(String period)`: tìm reading theo kỳ
- `getReadingsByContract(UUID contractId)`: danh sách readings

### Controller: `ElectricityReadingController`
- `POST /api/electricity-readings` → 201
- `GET /api/electricity-readings?period=YYYY-MM`

### DTOs
- `CreateElectricityReadingRequest`: `period` (pattern `YYYY-MM`), `meterValue` (@PositiveOrZero)
- `CreateReadingCommand`: clean internal record
- `ElectricityReadingResponse`: từ entity

### Exceptions
- `DuplicateReadingException` → 409 `DUPLICATE_READING`
- `MeterValueDecreasedException` → 422 `METER_VALUE_DECREASED`

---

## Bước 3 — Backend: Bills

Package: `com.khoa.roommanagement.billing.bills`

### Entity: `Bill`
- Factory method `createFrom(contract, reading, previousReading)`:
  - Snapshot toàn bộ giá từ contract
  - Tính `consumption = reading.meterValue - previousReading.meterValue`
  - Tính `electricityAmount = consumption * contract.electricityUnitPrice`
  - Tính `totalAmount = rentAmount + electricityAmount + waterFee + serviceFee`
  - `dueDate = contract.paymentDueDay` của tháng tương ứng kỳ
  - `status = PENDING`
- Method `isPending()`, getters

### Repository: `BillRepository`
- `Optional<Bill> findByContractIdAndPeriod(UUID, String)`
- `boolean existsByContractIdAndPeriod(UUID, String)`
- `Page<Bill> findByContractId(UUID, Pageable)` (phân trang 12)

### Service: `BillService`
- `create(CreateBillCommand)`:
  1. Kiểm tra hợp đồng ACTIVE → throw nếu không
  2. Kiểm tra chưa có bill cho kỳ → throw `DuplicateBillException` (409)
  3. Tìm reading của kỳ → throw nếu không có
  4. Tìm reading kỳ trước (làm old value) → throw nếu không có
  5. Tạo bill qua `Bill.createFrom()` → save
- `getById(UUID)` → chi tiết bill
- `getBills(String period, Pageable)` → danh sách

### Controller: `BillController`
- `POST /api/bills` → 201
- `GET /api/bills?period=YYYY-MM` (phân trang mặc định 12)
- `GET /api/bills/{id}`

### DTOs
- `CreateBillRequest`: `contractId`, `period` (YYYY-MM)
- `BillResponse`: toàn bộ fields snapshot + breakdown tiền

### Exceptions
- `DuplicateBillException` → 409 `DUPLICATE_BILL`
- `ReadingNotFoundException` → 422 `READING_NOT_FOUND`
- `PreviousReadingNotFoundException` → 422 `PREVIOUS_READING_NOT_FOUND`

---

## Bước 4 — Exception Handler

Mở rộng `ApiExceptionHandler` thêm handlers cho:
- `DuplicateReadingException` → 409
- `MeterValueDecreasedException` → 422
- `DuplicateBillException` → 409
- `ReadingNotFoundException` → 422
- `PreviousReadingNotFoundException` → 422

---

## Bước 5 — Frontend: API Types

File: `frontend/src/features/bills/api.ts`

```typescript
export type ElectricityReading = {
  id: string;
  contractId: string;
  period: string;
  meterValue: number;
  recordedAt: string;
};

export type Bill = {
  id: string;
  contractId: string;
  period: string;
  rentAmount: number;
  electricityUnitPrice: number;
  waterFee: number;
  serviceFee: number;
  oldMeterValue: number;
  newMeterValue: number;
  consumption: number;
  electricityAmount: number;
  totalAmount: number;
  status: "PENDING" | "PAID" | "OVERDUE";
  dueDate: string;
  createdAt: string;
};
```

API functions:
- `recordElectricityReading(period, meterValue)`
- `createBill(contractId, period)`
- `getBill(id)`
- `getBills(period?)`

---

## Bước 6 — Frontend: UI

Mở rộng `App.tsx` thêm 2 section:

1. **Form nhập chỉ số điện**: chọn kỳ (YYYY-MM), nhập chỉ số mới, nút "Ghi chỉ số"
2. **Tạo hóa đơn**: sau khi ghi chỉ số, nút "Tạo hóa đơn" → hiển thị breakdown chi tiết (tiền phòng, tiền điện, tiền nước, phí dịch vụ, tổng cộng)
3. **Danh sách hóa đơn**: hiển thị các kỳ đã tạo, trạng thái PENDING/PAID

Thêm CSS trong `App.css` cho các section mới.

---

## Bước 7 — Tests

### Unit tests
- `ElectricityReadingServiceTest`:
  - Từ chối chỉ số giảm
  - Từ chối trùng kỳ
  - Ghi nhận chỉ số hợp lệ
  - Từ chối khi hợp đồng bị hủy

- `BillServiceTest`:
  - Công thức tính tổng tiền (bảng tình huống: bình thường, consumption = 0, giá cao)
  - Snapshot giá không đổi
  - Từ chối trùng kỳ
  - Từ chối khi không có reading
  - Từ chối khi không có reading kỳ trước
  - Tính `dueDate` đúng theo `paymentDueDay`

### Controller tests
- `ElectricityReadingControllerTest`:
  - Validation error cho dữ liệu không hợp lệ
  - 409 cho trùng kỳ
  - 422 cho chỉ số giảm
  - 201 cho tạo thành công

- `BillControllerTest`:
  - 409 cho trùng kỳ
  - 422 cho không có reading
  - 201 cho tạo thành công

---

## Bước 8 — Commit chiến lược

Chia thành nhiều commit nhỏ:
1. `feat: add migration for electricity_readings and bills tables`
2. `feat: add electricity reading entity, repository and service`
3. `feat: add electricity reading controller and DTOs`
4. `feat: add bill entity, repository and service`
5. `feat: add bill controller and DTOs`
6. `feat: add exception handlers for readings and bills`
7. `test: add unit tests for electricity reading service`
8. `test: add unit tests for bill service`
9. `test: add controller tests for readings and bills`
10. `feat: add frontend API types for bills and readings`
11. `feat: add frontend UI for meter readings and bills`
12. `chore: tick done Task 3 in todo.md`

---

## Bước 9 — Cập nhật todo.md

Tick done Task 3 và checkpoint Tuần 1.

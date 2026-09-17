-- Script insert dữ liệu lịch sử từ 9/2024 đến 9/2026
-- Chạy script này sau khi đã tạo contract đầu tiên qua API

-- Bước 1: Drop CHECK constraints tạm thời (vì giá điện thay đổi theo kỳ, không phải hằng số)
ALTER TABLE bills DROP CONSTRAINT IF EXISTS chk_bills_electricity_amount;
ALTER TABLE bills DROP CONSTRAINT IF EXISTS chk_bills_total_amount;

-- Bước 2: Insert readings (chỉ số điện)
-- Kỳ 8/2024: chỉ số 0 (làm mốc ban đầu)
INSERT INTO electricity_readings (id, contract_id, period, meter_value, recorded_at)
SELECT gen_random_uuid(), id, '2024-08', 0, NOW()
FROM rental_contracts WHERE status = 'ACTIVE' LIMIT 1;

-- Các kỳ tiếp theo
INSERT INTO electricity_readings (id, contract_id, period, meter_value, recorded_at)
SELECT gen_random_uuid(), rc.id, r.period, r.meter_value, NOW()
FROM rental_contracts rc, (VALUES
  ('2024-09', 78),
  ('2024-10', 168),
  ('2024-11', 280),
  ('2024-12', 430),
  ('2025-01', 566),
  ('2025-02', 673),
  ('2025-03', 827),
  ('2025-04', 1057),
  ('2025-05', 1243),
  ('2025-06', 1423),
  ('2025-07', 1635),
  ('2025-08', 1851),
  ('2025-09', 2067),
  ('2025-10', 2193),
  ('2025-11', 2427),
  ('2025-12', 2627),
  ('2026-01', 2798),
  ('2026-02', 2973),
  ('2026-03', 3086),
  ('2026-04', 3292),
  ('2026-05', 3570),
  ('2026-06', 3796),
  ('2026-07', 4056),
  ('2026-08', 4321),
  ('2026-09', 4515)
) AS r(period, meter_value)
WHERE rc.status = 'ACTIVE';

-- Bước 3: Insert bills với giá trị chính xác từ dữ liệu
-- electricity_unit_price là giá trung bình (electricity_amount / consumption), có thể thay đổi theo kỳ
INSERT INTO bills (id, contract_id, period, rent_amount, electricity_unit_price, water_fee, service_fee, old_meter_value, new_meter_value, consumption, electricity_amount, total_amount, status, due_date, created_at)
SELECT gen_random_uuid(), rc.id, b.period, b.rent_amount, b.electricity_unit_price, b.water_fee, b.service_fee, b.old_meter_value, b.new_meter_value, b.consumption, b.electricity_amount, b.total_amount, 'PAID', b.due_date, NOW()
FROM rental_contracts rc, (VALUES
  -- 2024 (Phòng 4.200.000, Điện 3500/kWh)
  ('2024-09', 4200000, 3500, 200000, 100000, 0, 78, 78, 273000, 4773000, '2024-09-05'),
  ('2024-10', 4200000, 3500, 200000, 100000, 78, 168, 90, 315000, 4815000, '2024-10-05'),
  ('2024-11', 4200000, 3500, 200000, 100000, 168, 280, 112, 392000, 4892000, '2024-11-05'),
  ('2024-12', 4200000, 3500, 200000, 100000, 280, 430, 150, 525000, 5025000, '2024-12-05'),
  -- 2025 (Phòng 4.400.000 từ tháng 2, Điện 3500/kWh đến tháng 7)
  ('2025-01', 4200000, 3500, 200000, 100000, 430, 566, 136, 476000, 4976000, '2025-01-05'),
  ('2025-02', 4400000, 3500, 200000, 100000, 566, 673, 107, 374500, 5074500, '2025-02-05'),
  ('2025-03', 4400000, 3500, 200000, 100000, 673, 827, 154, 539000, 5239000, '2025-03-05'),
  ('2025-04', 4400000, 3500, 200000, 100000, 827, 1057, 230, 805000, 5505000, '2025-04-05'),
  ('2025-05', 4400000, 3500, 200000, 100000, 1057, 1243, 186, 651000, 5351000, '2025-05-05'),
  ('2025-06', 4400000, 3500, 200000, 100000, 1243, 1423, 180, 630000, 5330000, '2025-06-05'),
  ('2025-07', 4400000, 3500, 200000, 100000, 1423, 1635, 212, 742000, 5442000, '2025-07-05'),
  -- 2025-2026 (Điện thay đổi theo kỳ, có thể do giá bậc thang)
  ('2025-08', 4400000, 3801, 200000, 100000, 1635, 1851, 216, 821000, 5521000, '2025-08-05'),
  ('2025-09', 4400000, 3801, 200000, 100000, 1851, 2067, 216, 821000, 5521000, '2025-09-05'),
  ('2025-10', 4400000, 3802, 200000, 100000, 2067, 2193, 126, 479000, 5179000, '2025-10-05'),
  ('2025-11', 4400000, 3803, 200000, 100000, 2193, 2427, 234, 890000, 5590000, '2025-11-05'),
  ('2025-12', 4400000, 3800, 200000, 100000, 2427, 2627, 200, 760000, 5460000, '2025-12-05'),
  ('2026-01', 4400000, 3801, 200000, 100000, 2627, 2798, 171, 650000, 5350000, '2026-01-05'),
  ('2026-02', 4400000, 3800, 200000, 100000, 2798, 2973, 175, 665000, 5365000, '2026-02-05'),
  ('2026-03', 4400000, 3805, 200000, 100000, 2973, 3086, 113, 430000, 5130000, '2026-03-05'),
  ('2026-04', 4400000, 3801, 200000, 100000, 3086, 3292, 206, 783000, 5483000, '2026-04-05'),
  ('2026-05', 4400000, 3800, 200000, 100000, 3292, 3570, 278, 1056000, 5756000, '2026-05-05'),
  ('2026-06', 4400000, 3801, 200000, 100000, 3570, 3796, 226, 859000, 5559000, '2026-06-05'),
  ('2026-07', 4400000, 3800, 200000, 100000, 3796, 4056, 260, 988000, 5688000, '2026-07-05'),
  ('2026-08', 4400000, 3800, 200000, 100000, 4056, 4321, 265, 1007000, 5707000, '2026-08-05'),
  ('2026-09', 4400000, 3800, 200000, 100000, 4321, 4515, 194, 737000, 5437000, '2026-09-05')
) AS b(period, rent_amount, electricity_unit_price, water_fee, service_fee, old_meter_value, new_meter_value, consumption, electricity_amount, total_amount, due_date)
WHERE rc.status = 'ACTIVE';

-- Bước 4: Cập nhật contract hiện tại với giá trị mới nhất (9/2026)
UPDATE rental_contracts 
SET rent_amount = 4400000,
    electricity_unit_price = 3800,
    water_fee = 200000,
    service_fee = 100000
WHERE status = 'ACTIVE';

-- Xong! Kiểm tra kết quả
SELECT period, rent_amount, electricity_unit_price, electricity_amount, total_amount 
FROM bills 
ORDER BY period;

# Checklist MVP 4 Tuần

## Tuần 1 — Nền Tảng Và Hóa Đơn

## Task 1: Khởi tạo cấu trúc ứng dụng

**Mô tả:** Tạo hai ứng dụng `backend` và `frontend`, Docker Compose cho PostgreSQL/backend/frontend, biến môi trường mẫu, kiểm tra format/lint/test cơ bản.

**Tiêu chí chấp nhận:**

- [x] Backend và frontend khởi động độc lập bằng lệnh tài liệu hóa.
- [x] `docker compose up --build` khởi động được PostgreSQL, backend và frontend; database/chứng từ dùng volume bền vững.
- [x] Không có token, mật khẩu hoặc đường dẫn máy cá nhân trong source code.
- [x] Có migration rỗng đầu tiên và health endpoint nội bộ cho môi trường local.

**Kiểm chứng:**

- [x] `./mvnw verify -f backend/pom.xml` thành công.
- [x] `npm --prefix frontend run build` thành công.
- [x] `docker compose up --build` mở được giao diện và health endpoint.

**Phụ thuộc:** Không có.  
**Tệp dự kiến:** `backend/pom.xml`, `frontend/package.json`, `compose.yaml`, Dockerfile, `.env.example`.  
**Quy mô:** M.

## Task 2: Tạo hợp đồng và cấu hình phí

**Mô tả:** Hoàn thiện migration, entity, API và form mobile cho một hợp đồng đang hiệu lực.

**Tiêu chí chấp nhận:**

- [x] Không tạo được hai hợp đồng `ACTIVE`.
- [x] Ngày, tiền và đơn giá không hợp lệ nhận lỗi có cấu trúc.
- [x] Người dùng tạo và xem lại hợp đồng trên điện thoại.

**Kiểm chứng:**

- [x] Unit/integration test cho luật một hợp đồng active.
- [x] Manual: tạo hợp đồng từ giao diện và refresh vẫn còn dữ liệu.

**Phụ thuộc:** Task 1.  
**Tệp dự kiến:** module `billing`, migration hợp đồng, `frontend/src/features/contracts/`.  
**Quy mô:** M.

## Task 3: Nhập chỉ số và tạo hóa đơn

**Mô tả:** Ghi chỉ số điện, tính mức tiêu thụ và phát hành hóa đơn snapshot với tiền nước/phí dịch vụ cố định.

**Tiêu chí chấp nhận:**

- [x] Không chấp nhận chỉ số thấp hơn chỉ số kỳ trước hoặc kỳ trùng.
- [x] Tổng tiền theo công thức và snapshot không đổi khi sửa giá kỳ sau.
- [x] Giao diện hiển thị chi tiết tiền phòng, tiền điện theo tiêu thụ, tiền nước cố định và phí dịch vụ cố định.

**Kiểm chứng:**

- [x] Unit test bảng tình huống công thức và các biên chỉ số.
- [x] E2E: hợp đồng → hai chỉ số → một hóa đơn.

**Phụ thuộc:** Task 2.  
**Tệp dự kiến:** module `billing`, migration chỉ số/hóa đơn, `frontend/src/features/bills/`.  
**Quy mô:** M.

## Checkpoint: Sau Tuần 1

- [x] Toàn bộ test hiện có và build chạy thành công.
- [x] Demo tạo một hóa đơn trên màn hình điện thoại.
- [x] Review người dùng xác nhận tổng tiền trùng phép tính tay.

## Tuần 2 — Thanh Toán Và Chứng Từ

## Task 4: Xác nhận thanh toán và trạng thái đúng hạn

**Mô tả:** Tạo API/UI xác nhận thanh toán, idempotency và lịch sử phân trang.

**Tiêu chí chấp nhận:**

- [x] Hóa đơn chỉ được xác nhận một lần; retry cùng intent không tạo bản ghi thứ hai.
- [x] Trạng thái đúng hạn/trễ hạn được tính theo `paidAt` và hạn thanh toán.
- [x] Lịch sử hiển thị kỳ, số tiền, thời điểm và trạng thái.

**Kiểm chứng:**

- [x] Unit test biên đúng hạn/trễ hạn.
- [x] Integration test idempotency và lỗi `409`/`422`.

**Phụ thuộc:** Task 3.  
**Tệp dự kiến:** module `payments`, migration thanh toán, `frontend/src/features/payments/`.  
**Quy mô:** M.

## Task 5: Upload và xem ảnh chứng từ

**Mô tả:** Thêm lưu trữ local an toàn, API upload/stream và giao diện chọn ảnh.

**Tiêu chí chấp nhận:**

- [x] Chỉ nhận JPEG, PNG, WebP không quá 5 MB và đã kiểm tra chữ ký tệp.
- [x] Metadata không lộ đường dẫn local; tệp lỗi không còn trên ổ đĩa.
- [x] Người dùng mở lại ảnh từ lịch sử thanh toán.

**Kiểm chứng:**

- [x] Integration test loại tệp, kích thước và path traversal.
- [x] Manual: upload ảnh hợp lệ từ điện thoại và mở lại sau refresh.

**Phụ thuộc:** Task 4.  
**Tệp dự kiến:** module `payments`, cấu hình storage, `frontend/src/features/receipts/`.  
**Quy mô:** M.

## Checkpoint: Sau Tuần 2

- [x] Luồng hóa đơn → thanh toán → chứng từ chạy end-to-end.
- [x] Không có token/đường dẫn local trong API response hay log kiểm thử.
- [x] Dữ liệu thanh toán của một kỳ thực tế được đối chiếu thủ công.

## Tuần 3 — Nhắc Telegram

## Task 6: Cấu hình và gửi tin nhắn thử Telegram

**Mô tả:** Tạo adapter Telegram tách biệt, kiểm tra biến môi trường và màn hình gửi tin nhắn thử.

**Tiêu chí chấp nhận:**

- [x] Token/chat ID không được commit, log hoặc trả qua API.
- [x] Tin nhắn thử đến đúng chat cấu hình với timeout và lỗi an toàn.
- [x] Adapter được mock được trong test.

**Kiểm chứng:**

- [x] Test adapter với mock HTTP server.
- [x] Manual: nhận một tin nhắn thử Telegram thật.

**Phụ thuộc:** Task 1.  
**Tệp dự kiến:** module `reminders`, `.env.example`, `frontend/src/features/settings/`.  
**Quy mô:** M.

## Task 7: Scheduler nhắc hạn có chống gửi trùng

**Mô tả:** Tạo cấu hình ngày nhắc, job hằng ngày, lịch sử reminder và retry giới hạn.

**Tiêu chí chấp nhận:**

- [x] Tạo đúng loại nhắc trước hạn, quá hạn và hết hợp đồng theo cấu hình.
- [x] Chạy lại job không gửi trùng cùng một reminder trong ngày.
- [x] Ngày trễ thứ 1–3 có cảnh báo; ngày trễ thứ 4 chưa thanh toán sẽ hủy hợp đồng và chặn thanh toán mới.

**Kiểm chứng:**

- [x] Unit test ngày nhắc, ngưỡng ba ngày và hủy ở ngày trễ thứ tư với clock cố định.
- [x] Integration test unique constraint và retry `FAILED`.

**Phụ thuộc:** Task 3, Task 4, Task 6.  
**Tệp dự kiến:** module `reminders`, migration reminder, `frontend/src/features/reminders/`.  
**Quy mô:** M.

## Checkpoint: Sau Tuần 3

- [x] Telegram gửi thật và lịch sử `SENT`/`FAILED` hiển thị đúng.
- [x] Job chạy lại không tạo tin trùng.
- [x] Review timezone `Asia/Ho_Chi_Minh` và lịch nhắc mặc định.

## Tuần 4 — Hoàn Thiện Và Bàn Giao

## Task 8: Hoàn thiện trải nghiệm mobile-first

**Mô tả:** Tinh chỉnh luồng chính, trạng thái tải/lỗi/trống, điều hướng và accessibility.

**Tiêu chí chấp nhận:**

- [x] Luồng chính dùng tốt ở 375 px, 768 px và desktop.
- [x] Form có nhãn, lỗi dễ hiểu, focus rõ ràng và thao tác bằng bàn phím.
- [x] Không có nội dung bị cắt hoặc thao tác quan trọng chỉ dựa vào màu.

**Kiểm chứng:**

- [x] Manual responsive ở ba kích thước.
- [x] Browser test kiểm tra console error và keyboard navigation.

**Phụ thuộc:** Task 3, Task 5, Task 7.  
**Tệp dự kiến:** `frontend/src/app/`, component dùng chung, style/tokens.  
**Quy mô:** M.

## Task 9: Kiểm thử toàn luồng và kiểm tra chất lượng

**Mô tả:** Bổ sung E2E, chạy build/lint/test, rà soát bảo mật upload và configuration.

**Tiêu chí chấp nhận:**

- [x] Luồng hợp đồng → hóa đơn → thanh toán → chứng từ → nhắc thử chạy end-to-end.
- [x] Không còn lỗi mức critical/high từ native dependency audit có thể khai thác.
- [x] Build production thành công và không có lỗi console trong luồng chính.

**Kiểm chứng:**

- [x] Backend/frontend test, lint, build chạy thành công ở mức hiện có.
- [x] Rà soát thủ công checklist bảo mật và accessibility.

**Phụ thuộc:** Task 8.  
**Tệp dự kiến:** `backend/src/test/`, `frontend/e2e/`, CI/tài liệu kiểm thử.  
**Quy mô:** M.

## Task 10: Tài liệu chạy dự án và nội dung CV

**Mô tả:** Viết README, hướng dẫn cấu hình local, ảnh demo và mô tả ngắn cho CV.

**Tiêu chí chấp nhận:**

- [x] Người mới có thể chạy dự án theo README với `.env.example`.
- [x] README nêu rõ kiến trúc, giới hạn MVP và cách kiểm thử.
- [ ] Có mô tả CV nhấn vào business logic, scheduler, Telegram và bảo mật upload. *(người dùng yêu cầu bỏ khỏi README)*

**Kiểm chứng:**

- [x] Thực hiện lại hướng dẫn trong môi trường sạch.
- [x] Review tài liệu không chứa bí mật hoặc dữ liệu chứng từ thật.

**Phụ thuộc:** Task 9.  
**Tệp dự kiến:** `README.md`, `docs/`, ảnh chụp màn hình.  
**Quy mô:** M.

## Checkpoint: Hoàn Thành MVP

- [x] Tất cả tiêu chí trong ba đặc tả đều đạt.
- [x] Backend/frontend test, lint và build thành công.
- [x] Demo được toàn bộ luồng trên điện thoại.
- [x] Người dùng review và chấp nhận MVP trước khi sang Phase 2.

---

# Phase 2 — Tách Trang Chủ / App Riêng, Dashboard, Cài Đặt

Plan chi tiết: `docs/PHASE-2-PLAN.md`

## Task 11: Auth backend — SĐT + OTP Telegram, TOTP dự phòng

**Mô tả:** Spring Security + session JDBC; đăng nhập bằng SĐT chủ nhà + OTP gửi qua Telegram; dự phòng TOTP authenticator; rate-limit; seed tài khoản từ env.

**Tiêu chí chấp nhận:**

- [x] Toàn bộ `/api/**` (trừ auth + health) trả 401 khi chưa đăng nhập.
- [x] Request OTP: khớp SĐT mới gửi qua Telegram, rate-limit 3 lần/15 phút.
- [x] OTP sai/hết hạn nhận lỗi có cấu trúc; TOTP dự phòng bật được trong Settings.

**Kiểm chứng:**

- [x] Unit test OTP flow + rate-limit + TOTP với clock cố định.
- [x] Integration test: chưa đăng nhập 401 → đăng nhập → truy cập được API.

**Phụ thuộc:** MVP hoàn thành.  
**Tệp dự kiến:** module `auth`, migration owner_account, `SecurityConfig`.  
**Quy mô:** L.

## Task 12: Auth frontend — trang login 2 bước

**Mô tả:** Trang /login (SĐT → OTP), guard route, interceptor 401 redirect, logout.

**Tiêu chí chấp nhận:**

- [x] Chưa đăng nhập vào /app bị chuyển về /login; 401 từ API cũng vậy.
- [x] Login flow SĐT → OTP hoạt động; trạng thái lỗi hiển thị rõ.

**Kiểm chứng:**

- [x] Component test login flow; manual qua Docker.

**Phụ thuộc:** Task 11.  
**Tệp dự kiến:** `frontend/src/features/auth/`, `frontend/src/app/`.  
**Quy mô:** M.

## Task 13: App shell — Router + điều hướng

**Mô tả:** React Router; bottom nav mobile / sidebar desktop; dồn các section cũ vào /app (dashboard trống tạm), /app/bills, /app/payments.

**Tiêu chí chấp nhận:**

- [x] Không còn trang dọc dài; mỗi trang có skeleton/empty state riêng.
- [x] Bottom nav mobile, sidebar desktop; điều hướng keyboard tốt.

**Kiểm chứng:**

- [x] Browser test 375/768/1280; keyboard navigation.

**Phụ thuộc:** Task 12.  
**Tệp dự kiến:** `frontend/src/app/`, `frontend/src/components/layout/`.  
**Quy mô:** M.

## Task 14: Landing song ngữ + animation

**Mô tả:** Trang chủ portfolio VI/EN với i18n switcher, Framer Motion, lucide icons, dữ liệu mẫu hardcode, nút Đăng nhập.

**Tiêu chí chấp nhận:**

- [ ] Song ngữ chuyển đổi được, lưu localStorage; dữ liệu mẫu không gọi API.
- [ ] Animation chuyên nghiệp, không ảnh hưởng hiệu năng mobile.

**Kiểm chứng:**

- [ ] Browser test desktop + mobile; kiểm tra không có call API từ landing.

**Phụ thuộc:** Task 13.  
**Tệp dự kiến:** `frontend/src/features/landing/`, `frontend/src/i18n/`.  
**Quy mô:** L.

## Task 15: Dashboard

**Mô tả:** Endpoint `GET /api/dashboard/summary`; stat mới (tổng kỳ + tổng tiền đã bỏ ra, kỳ điện cao/thấp nhất); biểu đồ Recharts; danh sách sắp đến hạn.

**Tiêu chí chấp nhận:**

- [ ] Số liệu khớp tổng hợp từ hóa đơn thật; biểu đồ 12 tháng.
- [ ] Copy theo góc nhìn người thuê phòng.

**Kiểm chứng:**

- [ ] Unit test tổng hợp; manual với dữ liệu thật.

**Phụ thuộc:** Task 13.  
**Tệp dự kiến:** module `billing/dashboard`, `frontend/src/features/dashboard/`.  
**Quy mô:** M.

## Task 16: Trang Cài đặt

**Mô tả:** Gom hợp đồng (PATCH + gia hạn), Telegram, nhắc hạn, TOTP/tài khoản vào /app/settings.

**Tiêu chí chấp nhận:**

- [ ] PATCH hợp đồng áp dụng kỳ sau, snapshot cũ bất biến; gia hạn khi hết hạn.
- [ ] Bật/tắt TOTP trong Settings; đổi SĐT.

**Kiểm chứng:**

- [ ] Unit test PATCH + TOTP enable/disable; manual flow.

**Phụ thuộc:** Task 11, Task 13.  
**Tệp dự kiến:** module `billing/contracts`, `frontend/src/features/settings/`.  
**Quy mô:** M.

## Task 17: Icon + animation trong app + UX polish

**Mô tả:** lucide-react toàn app, chuyển trang mượt, FAB hành động chính, skeleton từng trang.

**Tiêu chí chấp nhận:**

- [ ] Icon thống nhất; animation tinh tế không phá tính công việc.

**Kiểm chứng:**

- [ ] Browser test mobile + desktop.

**Phụ thuộc:** Task 13.  
**Quy mô:** S.

## Task 18: Production — Oracle Cloud Free + Cloudflare

**Mô tả:** Guide mua domain, deploy lên Oracle Cloud Always Free (Docker Compose), Cloudflare DNS/HTTPS proxy, backup tự động volume, security review cuối.

**Tiêu chí chấp nhận:**

- [ ] Tài liệu từng bước chạy được từ máy sạch; backup/restore volume.

**Kiểm chứng:**

- [ ] Review tài liệu không chứa bí mật.

**Phụ thuộc:** Task 11–17.  
**Tệp dự kiến:** `docs/DEPLOYMENT.md`, `docker-compose.prod.yml`.  
**Quy mô:** S.

## Task 19: CI/CD GitHub Actions

**Mô tả:** CI push/PR: backend verify, frontend lint/test/build, build Docker; merge main: push ảnh GHCR; badge README.

**Tiêu chí chấp nhận:**

- [ ] CI xanh trên main; ảnh GHCR cập nhật sau merge.

**Kiểm chứng:**

- [ ] Workflow chạy thật trên GitHub; badge hiển thị.

**Phụ thuộc:** Task 10.  
**Tệp dự kiến:** `.github/workflows/ci.yml`.  
**Quy mô:** M.

## Task 20: README rewrite Phase 2

**Mô tả:** Cập nhật README song ngữ: auth, dashboard, trang chủ, CI badge, deploy production.

**Tiêu chí chấp nhận:**

- [ ] Hướng dẫn chạy từ đầu khớp thực tế Phase 2.

**Kiểm chứng:**

- [ ] Review không chứa bí mật.

**Phụ thuộc:** Task 11–19.  
**Quy mô:** S.

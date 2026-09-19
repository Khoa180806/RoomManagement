# ADR-004: Tách frontend theo feature và dùng Tailwind CSS

## Trạng Thái

Đã chấp nhận

## Ngày

2026-09-19

## Bối Cảnh

`frontend/src/App.tsx` trước đây chứa layout, toàn bộ form, lịch sử, gọi API,
validation và state của nhiều nghiệp vụ trong một file lớn. `App.css` cũng chứa
selector cho tất cả màn hình nên việc sửa một feature dễ ảnh hưởng feature khác.
Các API feature còn lặp lại logic `fetch`, timeout và đọc lỗi có cấu trúc.

Mục tiêu của đợt refactor là giảm chi phí bảo trì nhưng không thay đổi REST API,
quy tắc nghiệp vụ hoặc hành vi người dùng đã được xác nhận.

## Quyết Định

- Tổ chức mã theo bốn lớp: `app`, `components`, `features` và `shared`.
- `app/useRentalWorkspace.ts` là owner duy nhất của lifecycle tải contract trước,
  sau đó tải readings/bills/payments và cập nhật state dùng chung.
- Mỗi feature giữ API, validation và component của domain đó; component trình bày
  không tự gọi endpoint ngoài phần upload receipt được cô lập trong feature receipts.
- Dùng `shared/api/client.ts` cho timeout 10 giây và lỗi `{ error: { code,
  message, details } }`.
- Dùng Tailwind CSS v4 theo CSS-first, giữ design token semantic trong
  `shared/styles/tokens.css` và chỉ giữ global CSS tối thiểu.
- Giữ `src/App.tsx` là re-export để không phá import/entry point hiện tại.
- Thêm Vitest/jsdom và Testing Library cho unit/component test nhanh tại frontend.

## Lựa Chọn Bị Loại

### Giữ App.tsx và App.css nguyên khối

Cách này ít thay đổi ban đầu nhưng tiếp tục làm state, markup, API và style phụ
thuộc lẫn nhau; khó kiểm thử từng feature và khó truy tìm lỗi.

### Dùng CSS module riêng cho từng component

CSS module vẫn có thể phù hợp ở quy mô lớn, nhưng đợt này đã có design token và
layout utility rõ ràng. Tailwind v4 giúp style colocate trong component, loại bỏ
selector toàn cục dư thừa và không cần thêm file cấu hình lớn.

### Đưa toàn bộ server state vào global store

Workspace hiện chỉ có một luồng dữ liệu và chưa cần thêm thư viện state. Hook
điều phối cục bộ giữ dependency đơn giản, tránh tạo cache/invalidation mới ngoài
phạm vi refactor.

## Hệ Quả

- `App.tsx` giảm xuống còn entry re-export; composition chính nằm ở `app/App.tsx`.
- Component và validation có thể test độc lập mà không cần khởi động backend.
- Thay đổi CSS cần dùng token semantic và Tailwind class; không còn `App.css` để
  thêm selector tùy ý.
- Pagination lịch sử và receipt state vẫn giữ behavior cục bộ hiện tại; server
  pagination hoặc API lấy receipt sau refresh là các công việc riêng.
- Docker/browser smoke test vẫn cần được chạy ở môi trường có Docker/backend để
  xác nhận toàn luồng ngoài unit/component test.

# Frontend quản lý phòng trọ

## Mục đích

Frontend là giao diện React mobile-first cho hợp đồng thuê, chỉ số điện, hóa đơn,
thanh toán và chứng từ. REST API/backend không thay đổi trong đợt tái cấu trúc này.

## Cấu trúc thư mục

```text
src/
├── app/
│   ├── App.tsx                 # Ghép layout và các feature
│   └── useRentalWorkspace.ts   # Điều phối state/API cấp workspace
├── components/
│   ├── forms/                  # DateSelect, MonthSelect, Field
│   ├── data-display/           # Cost, Pagination, StatusPill
│   └── feedback/               # LoadingState, EmptyState, ErrorMessage
├── features/
│   ├── contracts/              # API, validation và form/card hợp đồng
│   ├── bills/                  # API, validation, readings và hóa đơn
│   ├── payments/               # API, form và lịch sử thanh toán
│   └── receipts/               # API và uploader chứng từ
├── shared/
│   ├── api/                    # request wrapper và lỗi có cấu trúc
│   ├── lib/                    # xử lý ngày/kỳ và format tiền/ngày
│   └── styles/                 # design tokens và global base CSS
└── test/                       # setup dùng chung cho Vitest
```

`src/App.tsx` chỉ re-export `src/app/App.tsx` để giữ entry point cũ. Component
nghiệp vụ nằm trong feature tương ứng; hook workspace là nơi duy nhất điều phối
lifecycle tải dữ liệu và cập nhật state server dùng chung.

## Quy ước phát triển

- API feature phải dùng `shared/api/client.ts`, không tự tạo `fetch` wrapper riêng.
- Component dùng chung nhận props thuần, không biết endpoint hoặc state server.
- Validation nghiệp vụ phía giao diện nằm trong `features/*/validation.ts`; backend
  vẫn là biên bảo mật và nguồn kiểm tra cuối cùng.
- Giá trị form được giữ dạng chuỗi cho đến khi API chuyển đổi sang số.
- Trạng thái và thông báo hiển thị bằng text, không chỉ dựa vào màu.
- Các class Tailwind trạng thái được khai báo tĩnh để Vite/Tailwind quét đầy đủ.
- `DateSelect` và `MonthSelect` giữ draft state từng phần để người dùng có thể
  chọn ngày/tháng/năm theo thứ tự bất kỳ mà không bị reset.

## Tailwind CSS

Dự án dùng Tailwind CSS v4 theo CSS-first:

- `src/shared/styles/tokens.css`: màu semantic, font và `@theme inline`.
- `src/shared/styles/globals.css`: nền trang và reset tối thiểu.
- `src/index.css`: ghép theme, tokens, utilities và global styles.

Không dùng `tailwind.config.js` hoặc PostCSS config riêng. `App.css` cũ đã được
xóa sau khi chuyển toàn bộ layout/component sang utility classes.

## Lệnh phát triển và kiểm thử

Chạy từ thư mục `frontend`:

```powershell
npm ci
npm run dev
```

Các lệnh kiểm tra:

```powershell
npm run lint       # oxlint
npm run test       # Vitest một lần
npm run test:watch # Vitest theo dõi thay đổi
npm run build      # TypeScript + Vite production build
```

Bộ test hiện có bao phủ helper ngày/kỳ, validation hợp đồng và hóa đơn,
`DateSelect` khi chọn từng phần, và điều hướng `Pagination`.

## Giới hạn hiện tại

- Ứng dụng chưa có authentication/authorization; không public trước khi bổ sung
  lớp bảo vệ này.
- Pagination lịch sử vẫn là pagination phía client theo dữ liệu workspace đã tải.
- Receipt uploader kiểm tra loại/kích thước ở client để hỗ trợ UX; backend vẫn kiểm
  tra magic bytes, kích thước và path traversal độc lập.

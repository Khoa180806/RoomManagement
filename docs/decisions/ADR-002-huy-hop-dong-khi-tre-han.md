# ADR-002: Hủy Hợp Đồng Khi Trễ Thanh Toán Quá Ba Ngày

## Trạng Thái

Đã chấp nhận

## Ngày

2026-09-13

## Bối Cảnh

Người dùng cần một quy tắc nghiêm để tránh trì hoãn thanh toán. Chỉ báo trạng thái trễ hạn không tạo đủ hậu quả rõ ràng.

## Quyết Định

- Thanh toán trễ tối đa ba ngày lịch sau hạn vẫn được chấp nhận và ghi nhận là trễ hạn.
- Nếu hóa đơn vẫn chưa thanh toán khi bước sang ngày trễ thứ tư, scheduler tự chuyển hợp đồng sang `TERMINATED_FOR_NON_PAYMENT`.
- Job gửi cảnh báo ở ngày trễ thứ 1, 2 và 3; mỗi cảnh báo nêu rõ hậu quả hủy hợp đồng ở ngày tiếp theo.
- Khi hợp đồng bị hủy, không phát sinh hóa đơn/chỉ số mới và không thể tạo thanh toán mới trong MVP.

## Hệ Quả

- Phải kiểm thử bằng clock/timezone cố định để tránh hủy sớm hoặc muộn.
- Cần lưu lịch sử nhắc và trạng thái hủy để có thể đối chiếu.
- Mọi nhu cầu khôi phục hợp đồng hoặc chấp nhận thanh toán sau khi hủy phải được thiết kế thành nghiệp vụ riêng.

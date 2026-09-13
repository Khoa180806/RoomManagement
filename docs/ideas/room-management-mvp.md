# MVP Quản Lý Phòng Trọ

## Vấn Đề Cần Giải Quyết

Làm sao để người thuê trọ luôn biết chính xác số tiền cần trả, nhận nhắc đúng lúc, và lưu đủ bằng chứng để đối chiếu thanh toán mà không phải ghi chép thủ công ở nhiều nơi?

## Hướng Đề Xuất

Xây dựng trợ lý hóa đơn phòng trọ cá nhân, không phải ứng dụng quản lý tài chính tổng quát. Người dùng tạo một hợp đồng thuê đang hiệu lực và cấu hình tiền phòng, đơn giá điện, tiền nước cố định, phí dịch vụ cố định. Mỗi kỳ, người dùng chỉ nhập chỉ số điện mới; ứng dụng tính mức tiêu thụ và tạo khoản cần thanh toán.

Thông báo Telegram nhắc người dùng trước ngày đến hạn thanh toán, khi quá hạn, và trước khi hợp đồng hết hạn. Sau khi trả tiền, người dùng ghi nhận thanh toán và tải ảnh chứng từ chuyển khoản. Ứng dụng lưu thời điểm thanh toán để tự động phân loại đúng hạn hay trễ hạn.

Người dùng được thanh toán trễ tối đa ba ngày theo lịch. Từ ngày trễ thứ tư, ứng dụng tự hủy hợp đồng vì không thanh toán và gửi thông báo Telegram nêu rõ hậu quả này.

Hướng này mang lại giá trị cá nhân rõ ràng trong một tháng, đồng thời thể hiện được mô hình dữ liệu, business logic tính hóa đơn, tác vụ lập lịch, tải tệp và tích hợp thông báo bên thứ ba.

## Nhu Cầu Người Dùng

Khi sắp đến kỳ đóng tiền trọ, tôi muốn biết chính xác số tiền cần trả và được nhắc đúng lúc, để thanh toán đúng hạn và đối chiếu lại khi cần.

## Giả Định Cần Kiểm Chứng

- [ ] Telegram bot có thể gửi tin nhắn đến chat của người dùng. Kiểm chứng trong tuần đầu bằng một thông báo end-to-end nhỏ.
- [ ] Người dùng sẽ nhập chỉ số điện hằng tháng. Luồng này cần mất dưới một phút.
- [ ] Đơn giá điện có thể thay đổi; tiền nước và phí dịch vụ cố định theo hợp đồng. Cần lưu lại toàn bộ giá trị được dùng ở mỗi hóa đơn để lịch sử tính toán luôn đúng.
- [ ] Ảnh chứng từ chuyển khoản cần được lưu trữ ổn định, không công khai. Bắt đầu bằng local storage có thể cấu hình cho MVP.
- [ ] Dữ liệu chi phí nhà trọ có thể tự động cấp cho Phase 2 quản lý dòng tiền cá nhân.

## Phạm Vi MVP

- Quản lý một hợp đồng thuê đang hiệu lực.
- Cấu hình tiền phòng, đơn giá điện, tiền nước cố định và phí dịch vụ cố định.
- Ghi nhận chỉ số điện theo kỳ; tính mức tiêu thụ và tổng hóa đơn.
- Tạo và xem lịch sử thanh toán.
- Ghi nhận thanh toán và tự động phân loại đúng hạn hay trễ hạn.
- Tải lên và xem ảnh chứng từ chuyển khoản.
- Gửi nhắc Telegram khi sắp đến hạn, quá hạn thanh toán và sắp hết hạn hợp đồng.
- Cảnh báo hậu quả khi trễ hạn và tự hủy hợp đồng khi trễ quá ba ngày.
- Giao diện responsive, ưu tiên thao tác trên điện thoại.

## Không Làm Trong MVP

- Nhiều phòng, người dùng, chủ trọ hoặc vai trò: độ phức tạp của phân quyền không cần thiết cho công cụ cá nhân.
- OCR chứng từ: tăng rủi ro kỹ thuật mà vẫn cần xác nhận dữ liệu hóa đơn.
- Chia tiền với bạn cùng phòng: thay đổi đáng kể business logic thanh toán.
- Dashboard dòng tiền, ngân sách và nhóm chi tiêu tổng quát: để sang Phase 2.
- Theo dõi điện theo ngày/thiết bị và gợi ý tối ưu: để sang Phase 3 sau khi có dữ liệu.
- Cloud storage phức tạp cho chứng từ: local storage cấu hình được là đủ cho MVP.

## Tiêu Chí Hoàn Thành

Sau một tháng, người dùng có thể dùng ứng dụng trên điện thoại để nhập chỉ số điện, xem chính xác số tiền cần trả, nhận nhắc Telegram, tải ảnh chứng từ chuyển khoản và xem lại các kỳ thanh toán cũ với trạng thái đúng hạn hay trễ hạn.

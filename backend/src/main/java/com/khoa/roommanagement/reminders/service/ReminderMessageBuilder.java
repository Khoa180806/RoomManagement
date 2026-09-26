package com.khoa.roommanagement.reminders.service;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Soạn nội dung thông điệp nhắc tiếng Việt. Chỉ gồm kỳ, hạn, tổng tiền,
 * trạng thái và ngày hết hạn — không gửi ảnh chứng từ hay dữ liệu thừa.
 */
@Component
public class ReminderMessageBuilder {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	public String billUpcoming(Bill bill) {
		return """
			🔔 Nhắc thanh toán tiền phòng

			Kỳ: %s
			Hạn thanh toán: %s
			Tổng tiền: %s đ
			Trạng thái: Chưa thanh toán""".formatted(
			bill.getPeriod(),
			DATE_FORMAT.format(bill.getDueDate()),
			formatMoney(bill.getTotalAmount()));
	}

	public String billOverdue(Bill bill, long daysPastDue) {
		return """
			⚠️ Hóa đơn quá hạn

			Kỳ: %s
			Hạn thanh toán: %s
			Tổng tiền: %s đ
			Trạng thái: Quá hạn %d ngày

			Nếu đến ngày trễ thứ 4 vẫn chưa thanh toán, hợp đồng sẽ bị hủy.""".formatted(
			bill.getPeriod(),
			DATE_FORMAT.format(bill.getDueDate()),
			formatMoney(bill.getTotalAmount()),
			daysPastDue);
	}

	public String contractTerminated(Bill bill) {
		return """
			❌ Hợp đồng đã bị hủy

			Hợp đồng bị hủy do hóa đơn kỳ %s chưa thanh toán quá 4 ngày kể từ hạn %s.
			Tổng tiền chưa thanh toán: %s đ.""".formatted(
			bill.getPeriod(),
			DATE_FORMAT.format(bill.getDueDate()),
			formatMoney(bill.getTotalAmount()));
	}

	public String contractExpiring(RentalContract contract, long daysUntilEnd) {
		return """
			📅 Hợp đồng sắp hết hạn

			Ngày kết thúc hợp đồng: %s
			Còn %d ngày nữa.""".formatted(
			DATE_FORMAT.format(contract.getEndDate()),
			daysUntilEnd);
	}

	private String formatMoney(long amount) {
		return java.text.NumberFormat.getIntegerInstance(java.util.Locale.of("vi", "VN")).format(amount);
	}
}

import type { RentalContractInput } from "./api";

export function validateRentalContract(form: RentalContractInput): string | null {
  const errors: string[] = [];
  if (!form.startDate) errors.push("Ngày bắt đầu là bắt buộc");
  if (!form.endDate) errors.push("Ngày kết thúc là bắt buộc");
  if (form.startDate && form.endDate && form.endDate <= form.startDate) {
    errors.push("Ngày kết thúc phải sau ngày bắt đầu");
  }
  if (!form.paymentDueDay || Number(form.paymentDueDay) < 1 || Number(form.paymentDueDay) > 28) {
    errors.push("Ngày đến hạn phải từ 1 đến 28");
  }
  if (form.rentAmount === "" || Number(form.rentAmount) < 0) {
    errors.push("Tiền phòng không được âm");
  }
  if (form.electricityUnitPrice === "" || Number(form.electricityUnitPrice) < 0) {
    errors.push("Đơn giá điện không được âm");
  }
  if (form.waterFee === "" || Number(form.waterFee) < 0) {
    errors.push("Tiền nước không được âm");
  }
  if (form.serviceFee === "" || Number(form.serviceFee) < 0) {
    errors.push("Phí dịch vụ không được âm");
  }
  return errors.length > 0 ? errors.join(". ") : null;
}

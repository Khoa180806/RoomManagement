export function validatePeriod(period: string): string | null {
  if (!period) return "Kỳ là bắt buộc";
  if (!/^\d{4}-\d{2}$/.test(period)) return "Kỳ phải có định dạng YYYY-MM";
  return null;
}

export function validateMeterValue(value: string): string | null {
  if (value === "" || Number(value) < 0) return "Chỉ số điện không được âm";
  return null;
}

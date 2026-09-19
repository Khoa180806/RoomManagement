export const money = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
});

export function formatMoney(value: number): string {
  return money.format(value);
}

export function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("vi-VN");
}

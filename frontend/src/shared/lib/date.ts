export const VIETNAMESE_MONTHS = [
  "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
  "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8",
  "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12",
] as const;

export const MIN_YEAR = 2024;
export const MAX_YEAR = new Date().getFullYear() + 5;

export function getCurrentPeriod(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

export function parsePeriod(period: string): { year: string; month: string } {
  if (/^\d{4}-\d{2}$/.test(period)) {
    const [year, month] = period.split("-");
    return { year, month };
  }
  return { year: "", month: "" };
}

export function parseDate(dateStr: string): {
  year: string;
  month: string;
  day: string;
} {
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
    const [year, month, day] = dateStr.split("-");
    return { year, month, day };
  }
  return { year: "", month: "", day: "" };
}

export function makePeriod(year: string, month: string): string {
  if (!year || !month) return "";
  return `${year}-${month.padStart(2, "0")}`;
}

export function makeDate(year: string, month: string, day: string): string {
  if (!year || !month || !day) return "";
  return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
}

export function getYears(): string[] {
  return Array.from({ length: MAX_YEAR - MIN_YEAR + 1 }, (_, index) =>
    String(MIN_YEAR + index),
  );
}

export function getDays(): string[] {
  return Array.from({ length: 31 }, (_, index) => String(index + 1));
}

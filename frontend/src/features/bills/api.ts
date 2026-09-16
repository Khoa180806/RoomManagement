const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";
export type ElectricityReading = {
  id: string;
  contractId: string;
  period: string;
  meterValue: number;
  recordedAt: string;
};
export type Bill = {
  id: string;
  contractId: string;
  period: string;
  rentAmount: number;
  electricityUnitPrice: number;
  waterFee: number;
  serviceFee: number;
  oldMeterValue: number;
  newMeterValue: number;
  consumption: number;
  electricityAmount: number;
  totalAmount: number;
  status: "PENDING" | "PAID" | "OVERDUE";
  dueDate: string;
  createdAt: string;
};
export type BillPage = {
  content: Bill[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
export type ReadingInput = {
  period: string;
  meterValue: string;
};
type ApiError = {
  error?: { code?: string; message?: string; details?: string[] };
};
export async function getElectricityReadings(): Promise<ElectricityReading[]> {
  return request("/api/electricity-readings");
}
export async function recordElectricityReading(
  input: ReadingInput,
): Promise<ElectricityReading> {
  return request("/api/electricity-readings", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      period: input.period,
      meterValue: Number(input.meterValue),
    }),
  });
}
export async function getBills(page = 0, size = 12): Promise<BillPage> {
  return request(`/api/bills?page=${page}&size=${size}`);
}
export async function createBill(period: string): Promise<Bill> {
  return request("/api/bills", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ period }),
  });
}
async function request(path: string, init?: RequestInit): Promise<any> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    signal: AbortSignal.timeout(10000),
  });
  if (response.ok) return response.json();
  const payload = (await response.json().catch(() => ({}))) as ApiError;
  if (payload.error?.code === "CONTRACT_TERMINATED") {
    throw new Error(payload.error.message ?? "Hợp đồng đã bị hủy.");
  }
  throw new Error(
    payload.error?.details?.join(" ") ||
      payload.error?.message ||
      "Không thể kết nối đến máy chủ.",
  );
}

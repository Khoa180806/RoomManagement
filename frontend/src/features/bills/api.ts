const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

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
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

type ApiError = {
  error?: { code?: string; message?: string; details?: string[] };
};

export async function recordElectricityReading(
  period: string,
  meterValue: number,
): Promise<ElectricityReading> {
  return request<ElectricityReading>("/api/electricity-readings", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ period, meterValue }),
  });
}

export async function getElectricityReadings(): Promise<ElectricityReading[]> {
  return request<ElectricityReading[]>("/api/electricity-readings");
}

export async function createBill(period: string): Promise<Bill> {
  return request<Bill>("/api/bills", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ period }),
  });
}

export async function getBills(page = 0, size = 12): Promise<BillPage> {
  return request<BillPage>(`/api/bills?page=${page}&size=${size}`);
}

export async function getBillById(id: string): Promise<Bill> {
  return request<Bill>(`/api/bills/${id}`);
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, init);
  if (response.ok) return response.json() as Promise<T>;
  const payload = (await response.json().catch(() => ({}))) as ApiError;
  throw new Error(
    payload.error?.details?.join(" ") ||
      payload.error?.message ||
      "Không thể kết nối đến máy chủ.",
  );
}

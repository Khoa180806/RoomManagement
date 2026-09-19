import { request } from "../../shared/api/client";

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
export async function getElectricityReadings(): Promise<ElectricityReading[]> {
  return request<ElectricityReading[]>("/api/electricity-readings");
}
export async function recordElectricityReading(
  input: ReadingInput,
): Promise<ElectricityReading> {
  return request<ElectricityReading>("/api/electricity-readings", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      period: input.period,
      meterValue: Number(input.meterValue),
    }),
  });
}
export async function getBills(page = 0, size = 12): Promise<BillPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  return request<BillPage>(`/api/bills?${params.toString()}`);
}
export async function createBill(period: string): Promise<Bill> {
  return request<Bill>("/api/bills", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ period }),
  });
}

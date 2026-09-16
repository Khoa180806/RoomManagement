const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";
export type RentalContract = {
  id: string;
  startDate: string;
  endDate: string;
  paymentDueDay: number;
  rentAmount: number;
  electricityUnitPrice: number;
  waterFee: number;
  serviceFee: number;
  status: "ACTIVE";
};
export type RentalContractInput = {
  startDate: string;
  endDate: string;
  paymentDueDay: string;
  rentAmount: string;
  electricityUnitPrice: string;
  waterFee: string;
  serviceFee: string;
};
type ApiError = {
  error?: { code?: string; message?: string; details?: string[] };
};
export async function getActiveRentalContract(): Promise<RentalContract> {
  return request("/api/contracts/active");
}
export async function createRentalContract(
  input: RentalContractInput,
): Promise<RentalContract> {
  return request("/api/contracts", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      ...input,
      paymentDueDay: Number(input.paymentDueDay),
      rentAmount: Number(input.rentAmount),
      electricityUnitPrice: Number(input.electricityUnitPrice),
      waterFee: Number(input.waterFee),
      serviceFee: Number(input.serviceFee),
    }),
  });
}
async function request(
  path: string,
  init?: RequestInit,
): Promise<RentalContract> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    signal: AbortSignal.timeout(10000),
  });
  if (response.ok) return response.json() as Promise<RentalContract>;
  const payload = (await response.json().catch(() => ({}))) as ApiError;
  if (payload.error?.code === "NOT_FOUND") throw new Error("NOT_FOUND");
  if (payload.error?.code === "CONTRACT_TERMINATED") {
    throw new Error(payload.error.message ?? "Hợp đồng đã bị hủy.");
  }
  throw new Error(
    payload.error?.details?.join(" ") ||
      payload.error?.message ||
      "Không thể kết nối đến máy chủ.",
  );
}

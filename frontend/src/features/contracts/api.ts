import { request } from "../../shared/api/client";

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

export async function getActiveRentalContract(): Promise<RentalContract> {
  return request<RentalContract>("/api/contracts/active");
}

export async function createRentalContract(input: RentalContractInput): Promise<RentalContract> {
  return request<RentalContract>("/api/contracts", {
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

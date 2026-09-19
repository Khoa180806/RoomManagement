import { describe, expect, it } from "vitest";
import { validateRentalContract } from "./validation";
import type { RentalContractInput } from "./api";

const validContract: RentalContractInput = {
  startDate: "2026-02-02",
  endDate: "2027-02-02",
  paymentDueDay: "4",
  rentAmount: "4400000",
  electricityUnitPrice: "3800",
  waterFee: "200000",
  serviceFee: "100000",
};

describe("contract validation", () => {
  it("accepts a complete contract", () => {
    expect(validateRentalContract(validContract)).toBeNull();
  });

  it("rejects an end date before the start date and an invalid due day", () => {
    expect(validateRentalContract({ ...validContract, endDate: "2026-01-01", paymentDueDay: "31" })).toContain("Ngày kết thúc phải sau ngày bắt đầu");
    expect(validateRentalContract({ ...validContract, paymentDueDay: "31" })).toContain("Ngày đến hạn phải từ 1 đến 28");
  });
});

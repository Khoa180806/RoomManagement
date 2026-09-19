import { describe, expect, it } from "vitest";
import { validatePeriod, validateMeterValue } from "./validation";

describe("bill validation", () => {
  it("accepts a valid period and meter value", () => {
    expect(validatePeriod("2025-02")).toBeNull();
    expect(validateMeterValue("123")).toBeNull();
  });

  it("returns Vietnamese messages for missing or invalid values", () => {
    expect(validatePeriod("")).toBe("Kỳ là bắt buộc");
    expect(validatePeriod("2025/02")).toBe("Kỳ phải có định dạng YYYY-MM");
    expect(validateMeterValue("-1")).toBe("Chỉ số điện không được âm");
  });
});

import { describe, expect, it } from "vitest";
import { getDays, getYears, makeDate, makePeriod, parseDate, parsePeriod } from "./date";

describe("date helpers", () => {
  it("parses and creates periods without losing zero padding", () => {
    expect(parsePeriod("2025-02")).toEqual({ year: "2025", month: "02" });
    expect(makePeriod("2025", "2")).toBe("2025-02");
  });

  it("rejects malformed dates by returning empty parts", () => {
    expect(parseDate("not-a-date")).toEqual({ year: "", month: "", day: "" });
    expect(makeDate("2025", "2", "3")).toBe("2025-02-03");
  });

  it("includes historical years from 2024 and all calendar day options", () => {
    expect(getYears()).toContain("2024");
    expect(getYears()).toContain(String(new Date().getFullYear() + 5));
    expect(getDays()).toHaveLength(31);
  });
});

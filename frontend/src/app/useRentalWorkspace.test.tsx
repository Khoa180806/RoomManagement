import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useRentalWorkspace } from "./useRentalWorkspace";

function notFoundResponse() {
  return new Response(
    JSON.stringify({ error: { code: "NOT_FOUND", message: "NOT_FOUND", details: [] } }),
    { status: 404, headers: { "Content-Type": "application/json" } },
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("useRentalWorkspace payment validation", () => {
  it("rejects a payment date in the future before calling the API", async () => {
    const fetchMock = vi.fn().mockResolvedValue(notFoundResponse());
    vi.stubGlobal("fetch", fetchMock);

    const { result } = renderHook(() => useRentalWorkspace());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => result.current.setSelectedBillId("bill-1"));
    act(() => result.current.setPaidAt("2031-12-31"));
    await act(() => result.current.confirmPaymentHandler({ preventDefault() {} }));

    expect(result.current.paymentError).toBe("Ngày thanh toán không được ở tương lai.");
    const paymentCalls = fetchMock.mock.calls.filter(([url]) => String(url).includes("/payments"));
    expect(paymentCalls).toHaveLength(0);
  });

  it("rejects a missing payment date", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(notFoundResponse()));

    const { result } = renderHook(() => useRentalWorkspace());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => result.current.setSelectedBillId("bill-1"));
    await act(() => result.current.confirmPaymentHandler({ preventDefault() {} }));

    expect(result.current.paymentError).toBe("Ngày thanh toán là bắt buộc.");
  });
});

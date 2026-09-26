import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SessionProvider } from "../SessionProvider";
import { LoginPage } from "./LoginPage";

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

// SessionProvider kiểm tra phiên (me) khi mount — luôn trả 401 để tests
// điều khiển luồng login từ đầu.
function stubSessionCheck() {
  return jsonResponse(401, {
    error: { code: "UNAUTHORIZED", message: "Bạn cần đăng nhập", details: [] },
  });
}

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={["/login"]}>
      <SessionProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/app" element={<p>APP_PAGE</p>} />
        </Routes>
      </SessionProvider>
    </MemoryRouter>,
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("LoginPage", () => {
  it("walks through the two-step phone + OTP login and lands in the app", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(stubSessionCheck())
      .mockResolvedValueOnce(jsonResponse(200, { sent: true, expiresInSeconds: 300 }))
      .mockResolvedValueOnce(jsonResponse(200, { authenticated: true }));
    vi.stubGlobal("fetch", fetchMock);

    renderLogin();
    await screen.findByLabelText("Số điện thoại");

    await user.type(screen.getByLabelText("Số điện thoại"), "0359955950");
    await user.click(screen.getByRole("button", { name: "Gửi mã OTP" }));

    expect(await screen.findByText(/Mã OTP đã gửi tới Telegram/)).toBeInTheDocument();
    expect(screen.getByLabelText("Mã xác thực")).toBeInTheDocument();

    await user.type(screen.getByLabelText("Mã xác thực"), "123456");
    await user.click(screen.getByRole("button", { name: "Đăng nhập" }));

    expect(await screen.findByText("APP_PAGE")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/auth/verify-otp",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ phone: "0359955950", code: "123456" }),
      }),
    );
  });

  it("shows the server error when the phone does not match", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(stubSessionCheck())
      .mockResolvedValueOnce(jsonResponse(422, {
        error: { code: "PHONE_MISMATCH", message: "Số điện thoại không khớp với chủ nhà.", details: [] },
      }));
    vi.stubGlobal("fetch", fetchMock);

    renderLogin();
    await screen.findByLabelText("Số điện thoại");

    await user.type(screen.getByLabelText("Số điện thoại"), "0999999999");
    await user.click(screen.getByRole("button", { name: "Gửi mã OTP" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Số điện thoại không khớp với chủ nhà.",
    );
    // Vẫn ở bước nhập SĐT
    expect(screen.getByLabelText("Số điện thoại")).toBeInTheDocument();
  });

  it("limits the OTP input to six digits", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(stubSessionCheck())
      .mockResolvedValueOnce(jsonResponse(200, { sent: true, expiresInSeconds: 300 }));
    vi.stubGlobal("fetch", fetchMock);

    renderLogin();
    await screen.findByLabelText("Số điện thoại");

    await user.type(screen.getByLabelText("Số điện thoại"), "0359955950");
    await user.click(screen.getByRole("button", { name: "Gửi mã OTP" }));
    await screen.findByText(/Mã OTP đã gửi tới Telegram/);

    const codeInput = screen.getByLabelText("Mã xác thực");
    await user.type(codeInput, "1234567890");

    expect(codeInput).toHaveValue("123456");
  });
});

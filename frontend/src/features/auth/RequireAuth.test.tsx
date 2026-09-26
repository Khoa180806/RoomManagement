import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SessionProvider } from "./SessionProvider";
import { RequireAuth } from "./RequireAuth";

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function renderGuarded() {
  return render(
    <MemoryRouter initialEntries={["/app"]}>
      <SessionProvider>
        <Routes>
          <Route
            path="/app"
            element={
              <RequireAuth>
                <p>APP_PAGE</p>
              </RequireAuth>
            }
          />
          <Route path="/login" element={<p>LOGIN_PAGE</p>} />
        </Routes>
      </SessionProvider>
    </MemoryRouter>,
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("RequireAuth", () => {
  it("redirects to login when the session check returns 401", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      jsonResponse(401, {
        error: { code: "UNAUTHORIZED", message: "Bạn cần đăng nhập", details: [] },
      }),
    ));

    renderGuarded();

    expect(await screen.findByText("LOGIN_PAGE")).toBeInTheDocument();
    expect(screen.queryByText("APP_PAGE")).not.toBeInTheDocument();
  });

  it("renders protected content when authenticated", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      jsonResponse(200, { phone: "912****678", totpEnabled: false }),
    ));

    renderGuarded();

    expect(await screen.findByText("APP_PAGE")).toBeInTheDocument();
    expect(screen.queryByText("LOGIN_PAGE")).not.toBeInTheDocument();
  });

  it("shows a skeleton while the session check is in flight", () => {
    vi.stubGlobal("fetch", vi.fn().mockReturnValue(new Promise(() => {})));

    renderGuarded();

    expect(screen.getByRole("status", { name: "Đang kiểm tra phiên đăng nhập" })).toBeInTheDocument();
    expect(screen.queryByText("LOGIN_PAGE")).not.toBeInTheDocument();
    expect(screen.queryByText("APP_PAGE")).not.toBeInTheDocument();
  });
});

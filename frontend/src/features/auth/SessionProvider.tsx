import { useEffect, useState, type ReactNode } from "react";
import { ApiRequestError } from "../../shared/api/errors";
import { getMe } from "./api";
import { SessionContext, type SessionStatus } from "./SessionContext";

export function SessionProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<SessionStatus>("loading");

  useEffect(() => {
    let cancelled = false;
    getMe()
      .then(() => {
        if (!cancelled) setStatus("authenticated");
      })
      .catch((error: unknown) => {
        // 401 là trạng thái bình thường khi chưa đăng nhập; lỗi khác
        // (backend sập) cũng đưa về màn login để người dùng thấy rõ.
        if (!cancelled) setStatus("unauthenticated");
        if (!(error instanceof ApiRequestError && error.code === "UNAUTHORIZED")) {
          console.warn("Không kiểm tra được phiên đăng nhập", error);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <SessionContext.Provider
      value={{
        status,
        markAuthenticated: () => setStatus("authenticated"),
        markUnauthenticated: () => setStatus("unauthenticated"),
      }}
    >
      {children}
    </SessionContext.Provider>
  );
}

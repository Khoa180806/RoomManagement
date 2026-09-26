import { Navigate, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { useSession } from "./SessionContext";

/**
 * Chặn truy cập các route riêng tư: chưa đăng nhập thì về /login, giữ lại
 * trang người dùng định mở để đăng nhập xong quay lại đúng chỗ.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useSession();
  const location = useLocation();

  if (status === "loading") {
    return (
      <div
        className="grid min-h-dvh place-items-center bg-paper"
        role="status"
        aria-busy="true"
        aria-label="Đang kiểm tra phiên đăng nhập"
      >
        <div className="w-64 space-y-3">
          <span className="block h-10 animate-pulse bg-line" />
          <span className="block h-10 w-4/5 animate-pulse bg-line" />
        </div>
      </div>
    );
  }

  if (status === "unauthenticated") {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return children;
}

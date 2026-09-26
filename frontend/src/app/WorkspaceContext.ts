import { createContext, useContext } from "react";
import { useRentalWorkspace } from "./useRentalWorkspace";

/**
 * State của toàn bộ workspace (contract, readings, bills, payments) được
 * nạp một lần ở AppShell và chia sẻ cho mọi trang con — chuyển tab không
 * mất dữ liệu, không gọi lại API.
 */
export const WorkspaceContext = createContext<ReturnType<typeof useRentalWorkspace> | null>(null);

export function useWorkspace() {
  const value = useContext(WorkspaceContext);
  if (!value) {
    throw new Error("useWorkspace phải dùng bên trong WorkspaceProvider");
  }
  return value;
}

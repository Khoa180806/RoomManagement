import type { ReactNode } from "react";
import { useRentalWorkspace } from "./useRentalWorkspace";
import { WorkspaceContext } from "./WorkspaceContext";

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const workspace = useRentalWorkspace();
  return <WorkspaceContext.Provider value={workspace}>{children}</WorkspaceContext.Provider>;
}

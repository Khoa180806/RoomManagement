import { createContext, useContext } from "react";

export type SessionStatus = "loading" | "authenticated" | "unauthenticated";

export type SessionContextValue = {
  status: SessionStatus;
  markAuthenticated: () => void;
  markUnauthenticated: () => void;
};

export const SessionContext = createContext<SessionContextValue | null>(null);

export function useSession(): SessionContextValue {
  const value = useContext(SessionContext);
  if (!value) {
    throw new Error("useSession phải dùng bên trong SessionProvider");
  }
  return value;
}

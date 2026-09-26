import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import "./index.css";
import App from "./App.tsx";
import { SessionProvider } from "./features/auth/SessionProvider";
import { RequireAuth } from "./features/auth/RequireAuth";
import { LoginPage } from "./features/auth/components/LoginPage.tsx";
import { DashboardPage } from "./features/dashboard/pages/DashboardPage.tsx";
import { BillsPage } from "./features/bills/pages/BillsPage.tsx";
import { PaymentsPage } from "./features/payments/pages/PaymentsPage.tsx";
import { SettingsPage } from "./features/settings/pages/SettingsPage.tsx";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <SessionProvider>
        <Routes>
          {/* Trang chủ portfolio (Task 14) — tạm thời chuyển thẳng vào app */}
          <Route path="/" element={<Navigate to="/app" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/app"
            element={
              <RequireAuth>
                <App />
              </RequireAuth>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="bills" element={<BillsPage />} />
            <Route path="payments" element={<PaymentsPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/app" replace />} />
        </Routes>
      </SessionProvider>
    </BrowserRouter>
  </StrictMode>,
);

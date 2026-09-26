import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { LayoutDashboard, ReceiptText, Settings, Wallet } from "lucide-react";
import { ContractForm } from "../features/contracts/components/ContractForm";
import { LoadingState } from "../components/feedback/Feedback";
import { logout } from "../features/auth/api";
import { useSession } from "../features/auth/SessionContext";
import { WorkspaceProvider } from "./WorkspaceProvider";
import { useWorkspace } from "./WorkspaceContext";

const NAV_ITEMS = [
  { to: "/app", label: "Tổng quan", icon: LayoutDashboard, end: true },
  { to: "/app/bills", label: "Hóa đơn", icon: ReceiptText, end: false },
  { to: "/app/payments", label: "Thanh toán", icon: Wallet, end: false },
  { to: "/app/settings", label: "Cài đặt", icon: Settings, end: false },
];

const desktopLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-[0.35rem] px-3 py-2 text-sm font-semibold transition focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3 ${
    isActive ? "bg-ink text-white" : "text-muted hover:bg-sand hover:text-ink"
  }`;

function ShellContent() {
  const workspace = useWorkspace();
  const navigate = useNavigate();
  const { markUnauthenticated } = useSession();

  async function handleLogout() {
    try {
      await logout();
    } finally {
      markUnauthenticated();
      navigate("/login", { replace: true });
    }
  }

  return (
    <div className="min-h-dvh">
      <header className="sticky top-0 z-40 border-b border-line bg-paper">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 md:px-6">
          <span className="flex items-center gap-2.5 text-sm font-bold text-ink">
            <span className="rounded-[0.4rem] bg-ink p-[0.33rem] font-display text-[0.7rem] tracking-[0.05em] text-white" aria-hidden="true">RM</span>
            <span>Nhà trọ của tôi</span>
          </span>

          <nav className="hidden items-center gap-1 md:flex" aria-label="Menu điều hướng">
            {NAV_ITEMS.map(({ to, label, end }) => (
              <NavLink key={to} to={to} end={end} className={desktopLinkClass}>
                {label}
              </NavLink>
            ))}
          </nav>

          <button
            type="button"
            className="text-xs font-bold text-muted hover:text-clay hover:underline focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3"
            onClick={() => void handleLogout()}
          >
            Đăng xuất
          </button>
        </div>
      </header>

      <main className="mx-auto w-full max-w-4xl px-4 pt-6 pb-28 md:px-6 md:pb-10" id="main-content">
        {workspace.isLoading ? (
          <LoadingState />
        ) : workspace.contract ? (
          <Outlet />
        ) : (
          <section className="mx-auto max-w-2xl" aria-labelledby="setup-title">
            <div className="mb-6">
              <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Thiết lập ban đầu</p>
              <h1 className="mt-1 font-display text-2xl tracking-[-0.025em] text-ink">Tạo hợp đồng đầu tiên</h1>
              <p className="mt-2 text-sm text-muted">Lưu cấu hình một lần để tiền phòng, điện, nước và dịch vụ luôn được tính đúng.</p>
            </div>
            <ContractForm
              form={workspace.form}
              error={workspace.error}
              isSaving={workspace.isSaving}
              onSubmit={workspace.submitContract}
              onChange={workspace.updateField}
            />
          </section>
        )}
      </main>

      <nav
        className="fixed inset-x-0 bottom-0 z-40 grid grid-cols-4 border-t border-line bg-paper md:hidden"
        aria-label="Điều hướng chính"
      >
        {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `grid justify-items-center gap-1 py-2.5 text-[0.7rem] font-semibold transition focus-visible:outline-3 focus-visible:outline-clay ${
                isActive ? "text-clay" : "text-muted"
              }`
            }
          >
            <Icon size={20} aria-hidden="true" />
            {label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}

export function App() {
  return (
    <WorkspaceProvider>
      <ShellContent />
    </WorkspaceProvider>
  );
}

export default App;

import { useEffect, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useSession } from "../SessionContext";
import { requestOtp, verifyOtp, verifyTotp } from "../api";

type Step = "phone" | "otp" | "totp";

const inputClassName = "min-h-12 rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus";

export function LoginPage() {
  const { status, markAuthenticated } = useSession();
  const navigate = useNavigate();
  const location = useLocation();

  const [step, setStep] = useState<Step>("phone");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [secondsLeft, setSecondsLeft] = useState(0);

  useEffect(() => {
    if (status === "authenticated") {
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && from !== "/login" ? from : "/app", { replace: true });
    }
  }, [status, navigate, location.state]);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = setInterval(() => setSecondsLeft((value) => Math.max(0, value - 1)), 1000);
    return () => clearInterval(timer);
  }, [secondsLeft]);

  if (status === "loading") {
    return (
      <main className="grid min-h-dvh place-items-center bg-paper">
        <p className="text-sm text-muted" role="status">Đang tải…</p>
      </main>
    );
  }

  if (status === "authenticated") {
    return <Navigate to="/app" replace />;
  }

  async function handleRequestOtp(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setIsLoading(true);
    try {
      const result = await requestOtp(phone);
      setSecondsLeft(result.expiresInSeconds);
      setStep("otp");
    } catch (requestError: unknown) {
      setError(requestError instanceof Error ? requestError.message : "Không thể gửi mã OTP.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleVerify(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setIsLoading(true);
    try {
      if (step === "otp") {
        await verifyOtp(phone, code);
      } else {
        await verifyTotp(code);
      }
      markAuthenticated();
      navigate("/app", { replace: true });
    } catch (requestError: unknown) {
      setError(requestError instanceof Error ? requestError.message : "Đăng nhập thất bại.");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <main className="grid min-h-dvh place-items-center bg-paper px-4">
      <section className="w-full max-w-sm border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)]" aria-labelledby="login-title">
        <div className="mb-6 text-center">
          <span className="inline-block rounded-[0.4rem] bg-ink px-2 py-1 font-display text-[0.7rem] tracking-[0.05em] text-white" aria-hidden="true">RM</span>
          <h1 id="login-title" className="mt-3 font-display text-2xl tracking-[-0.025em] text-ink">Đăng nhập</h1>
          <p className="mt-2 text-sm text-muted">
            {step === "phone" && "Nhập số điện thoại của chủ nhà để nhận mã OTP qua Telegram."}
            {step === "otp" && `Mã OTP đã gửi tới Telegram. Nhập 6 số để tiếp tục.`}
            {step === "totp" && "Nhập mã 6 số từ ứng dụng authenticator."}
          </p>
        </div>

        {error && (
          <p className="mb-4 border-l-4 border-danger bg-danger-bg p-3 text-sm text-danger-ink" role="alert">{error}</p>
        )}

        {step === "phone" && (
          <form onSubmit={handleRequestOtp} noValidate>
            <label className="grid gap-2 text-sm font-semibold text-ink" htmlFor="login-phone">
              Số điện thoại
              <input
                id="login-phone"
                className={inputClassName}
                type="tel"
                inputMode="numeric"
                autoComplete="tel"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                placeholder="09xx xxx xxx"
                required
              />
            </label>
            <button
              className="mt-5 min-h-[3.2rem] w-full rounded-[0.35rem] bg-ink px-5 text-[0.95rem] font-bold text-white transition hover:bg-clay focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3 disabled:cursor-wait disabled:opacity-65"
              type="submit"
              disabled={isLoading || phone.trim() === ""}
            >
              {isLoading ? "Đang gửi…" : "Gửi mã OTP"}
            </button>
          </form>
        )}

        {step !== "phone" && (
          <form onSubmit={handleVerify} noValidate>
            <label className="grid gap-2 text-sm font-semibold text-ink" htmlFor="login-code">
              Mã xác thực
              <input
                id="login-code"
                className={`${inputClassName} text-center text-lg tracking-[0.4em]`}
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                value={code}
                onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))}
                placeholder="••••••"
                required
              />
            </label>
            {step === "otp" && secondsLeft > 0 && (
              <p className="mt-2 text-xs text-muted" role="status">Mã hết hạn sau {Math.floor(secondsLeft / 60)}:{String(secondsLeft % 60).padStart(2, "0")}</p>
            )}
            <button
              className="mt-5 min-h-[3.2rem] w-full rounded-[0.35rem] bg-ink px-5 text-[0.95rem] font-bold text-white transition hover:bg-clay focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3 disabled:cursor-wait disabled:opacity-65"
              type="submit"
              disabled={isLoading || code.length !== 6}
            >
              {isLoading ? "Đang xác thực…" : "Đăng nhập"}
            </button>
            <button
              className="mt-3 w-full text-center text-xs font-semibold text-clay hover:underline"
              type="button"
              onClick={() => { setStep("phone"); setCode(""); setError(null); }}
            >
              ← Dùng số điện thoại khác
            </button>
          </form>
        )}

        {step !== "totp" && (
          <button
            className="mt-4 w-full text-center text-xs font-semibold text-muted hover:underline"
            type="button"
            onClick={() => { setStep("totp"); setCode(""); setError(null); }}
          >
            Đăng nhập bằng authenticator (TOTP)
          </button>
        )}
      </section>
    </main>
  );
}

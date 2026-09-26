import { request } from "../../shared/api/client";

export type MeResponse = {
  phone: string;
  totpEnabled: boolean;
};

export async function requestOtp(phone: string): Promise<{ sent: boolean; expiresInSeconds: number }> {
  return request("/api/auth/request-otp", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ phone }),
  });
}

export async function verifyOtp(phone: string, code: string): Promise<{ authenticated: boolean }> {
  return request("/api/auth/verify-otp", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ phone, code }),
  });
}

export async function verifyTotp(code: string): Promise<{ authenticated: boolean }> {
  return request("/api/auth/verify-totp", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ code }),
  });
}

export async function getMe(): Promise<MeResponse> {
  return request<MeResponse>("/api/auth/me");
}

export async function logout(): Promise<void> {
  await request("/api/auth/logout", { method: "POST" });
}

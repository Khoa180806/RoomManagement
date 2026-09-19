import { ApiRequestError, type ApiErrorPayload } from "./errors";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    signal: AbortSignal.timeout(10000),
  });

  if (response.ok) {
    return response.json() as Promise<T>;
  }

  const payload = (await response.json().catch(() => ({}))) as ApiErrorPayload;
  const code = payload.error?.code;
  if (code === "NOT_FOUND") {
    throw new ApiRequestError("NOT_FOUND", code);
  }

  throw new ApiRequestError(
    payload.error?.details?.join(" ") ||
      payload.error?.message ||
      "Không thể kết nối đến máy chủ.",
    code,
  );
}

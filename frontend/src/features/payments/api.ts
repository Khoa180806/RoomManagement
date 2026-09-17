const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";

export type Payment = {
  id: string;
  bill: {
    id: string;
    period: string;
    totalAmount: number;
    dueDate: string;
    status: string;
  };
  paidAt: string;
  note: string | null;
  onTime: boolean;
  createdAt: string;
};

export type PaymentInput = {
  paidAt: string;
  note?: string;
  idempotencyKey?: string;
};

export type PaymentPage = {
  content: Payment[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type ApiError = {
  error?: { code?: string; message?: string; details?: string[] };
};

export async function confirmPayment(
  billId: string,
  input: PaymentInput,
): Promise<Payment> {
  return request(`/api/bills/${billId}/payments`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}

export async function getPayments(
  page = 0,
  size = 12,
  onTime?: boolean,
): Promise<PaymentPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (onTime !== undefined) {
    params.set("onTime", String(onTime));
  }
  return request(`/api/payments?${params.toString()}`);
}

export async function getPaymentById(id: string): Promise<Payment> {
  return request(`/api/payments/${id}`);
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    signal: AbortSignal.timeout(10000),
  });
  if (response.ok) return response.json() as Promise<T>;
  const payload = (await response.json().catch(() => ({}))) as ApiError;
  throw new Error(
    payload.error?.details?.join(" ") ||
      payload.error?.message ||
      "Không thể kết nối đến máy chủ.",
  );
}

import { request } from "../../shared/api/client";

export type PaymentReceiptSummary = {
  id: string;
  contentType: string;
  fileSize: number;
};

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
  receipt: PaymentReceiptSummary | null;
};

export type PaymentInput = {
  paidAt: string;
  note?: string;
};

export type PaymentPage = {
  content: Payment[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export async function confirmPayment(
  billId: string,
  input: PaymentInput,
  idempotencyKey: string,
): Promise<Payment> {
  return request<Payment>(`/api/bills/${billId}/payments`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": idempotencyKey,
    },
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
  return request<PaymentPage>(`/api/payments?${params.toString()}`);
}

export async function getPaymentById(id: string): Promise<Payment> {
  return request<Payment>(`/api/payments/${id}`);
}

import { request } from "../../shared/api/client";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";

type Receipt = {
  id: string;
  paymentId: string;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
  downloadUrl: string;
};

export async function uploadReceipt(paymentId: string, file: File): Promise<Receipt> {
  const formData = new FormData();
  formData.append("file", file);

  return request<Receipt>(`/api/payments/${paymentId}/receipts`, {
    method: "POST",
    body: formData,
  });
}

export function getReceiptUrl(receiptId: string): string {
  return `${apiBaseUrl}/api/receipts/${receiptId}`;
}

export type { Receipt };

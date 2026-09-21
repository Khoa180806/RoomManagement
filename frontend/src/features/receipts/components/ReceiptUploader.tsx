import { useState } from "react";
import { ErrorMessage } from "../../../components/feedback/Feedback";
import { getReceiptUrl, uploadReceipt } from "../api";

type ReceiptUploaderProps = {
  paymentId: string;
  receiptId: string | null;
  onUploaded: (receiptId: string) => void;
};

export function ReceiptUploader({ paymentId, receiptId, onUploaded }: ReceiptUploaderProps) {
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleChange(file: File | undefined) {
    if (!file) return;
    setError(null);
    if (file.size > 5 * 1024 * 1024) {
      setError("Ảnh chứng từ không được vượt quá 5 MB.");
      return;
    }
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      setError("Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP.");
      return;
    }

    setIsUploading(true);
    try {
      const receipt = await uploadReceipt(paymentId, file);
      onUploaded(receipt.id);
    } catch (requestError: unknown) {
      setError(requestError instanceof Error ? requestError.message : "Không thể tải chứng từ lên.");
    } finally {
      setIsUploading(false);
    }
  }

  return (
    <div className="mt-1 flex flex-wrap items-center gap-2">
      <label className="cursor-pointer text-xs font-bold text-clay hover:underline" htmlFor={`receipt-${paymentId}`}>
        {isUploading ? "Đang tải..." : receiptId ? "Đổi chứng từ" : "Thêm chứng từ"}
      </label>
      <input
        id={`receipt-${paymentId}`}
        className="sr-only"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        disabled={isUploading}
        onChange={(event) => {
          const file = event.target.files?.[0];
          event.currentTarget.value = "";
          void handleChange(file);
        }}
      />
      {receiptId && <a className="text-xs font-bold text-clay hover:underline" href={getReceiptUrl(receiptId)} target="_blank" rel="noreferrer">Mở ảnh</a>}
      {error && <div className="basis-full"><ErrorMessage message={error} /></div>}
    </div>
  );
}

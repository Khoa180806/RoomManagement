import { useEffect, useRef, useState, type FormEvent } from "react";
import {
  createRentalContract,
  getActiveRentalContract,
  type RentalContract,
  type RentalContractInput,
} from "../features/contracts/api";
import { validateRentalContract } from "../features/contracts/validation";
import {
  createBill,
  getBills,
  getElectricityReadings,
  recordElectricityReading,
  type Bill,
  type ElectricityReading,
} from "../features/bills/api";
import { validateMeterValue, validatePeriod } from "../features/bills/validation";
import { confirmPayment, getPayments, type Payment } from "../features/payments/api";
import { ApiRequestError } from "../shared/api/errors";
import { getCurrentPeriod } from "../shared/lib/date";

const PAYMENT_PAGE_SIZE = 6;

const initialForm: RentalContractInput = {
  startDate: "",
  endDate: "",
  paymentDueDay: "4",
  rentAmount: "",
  electricityUnitPrice: "",
  waterFee: "",
  serviceFee: "",
};

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback;
}

function createIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `payment-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function useRentalWorkspace() {
  const [contract, setContract] = useState<RentalContract | null>(null);
  const [form, setForm] = useState<RentalContractInput>(initialForm);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [readings, setReadings] = useState<ElectricityReading[]>([]);
  const [readingPeriod, setReadingPeriod] = useState(getCurrentPeriod);
  const [readingMeterValue, setReadingMeterValue] = useState("");
  const [readingError, setReadingError] = useState<string | null>(null);
  const [isRecordingReading, setIsRecordingReading] = useState(false);
  const [bills, setBills] = useState<Bill[]>([]);
  const [billPeriod, setBillPeriod] = useState(getCurrentPeriod);
  const [billError, setBillError] = useState<string | null>(null);
  const [isCreatingBill, setIsCreatingBill] = useState(false);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [paymentPage, setPaymentPage] = useState(0);
  const [paymentTotalPages, setPaymentTotalPages] = useState(1);
  const [paymentOnTime, setPaymentOnTime] = useState<boolean | undefined>(undefined);
  const [isLoadingPayments, setIsLoadingPayments] = useState(false);
  const [selectedBillId, setSelectedBillIdState] = useState<string | null>(null);
  const [paidAt, setPaidAtState] = useState("");
  const [paymentNote, setPaymentNoteState] = useState("");
  const [paymentIdempotencyKey, setPaymentIdempotencyKey] = useState<string | null>(null);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [isConfirmingPayment, setIsConfirmingPayment] = useState(false);
  const hasLoadedRef = useRef(false);

  async function loadPayments(page: number, onTime: boolean | undefined = paymentOnTime) {
    setIsLoadingPayments(true);
    try {
      const data = await getPayments(page, PAYMENT_PAGE_SIZE, onTime);
      setPayments(data.content);
      setPaymentPage(data.number);
      setPaymentTotalPages(Math.max(1, data.totalPages));
    } catch (requestError: unknown) {
      setPaymentError(getErrorMessage(requestError, "Không thể tải lịch sử thanh toán."));
    } finally {
      setIsLoadingPayments(false);
    }
  }

  useEffect(() => {
    if (hasLoadedRef.current) return;
    hasLoadedRef.current = true;

    getActiveRentalContract()
      .then((activeContract) => {
        setContract(activeContract);
        return Promise.all([
          getElectricityReadings(),
          getBills(0, 100),
          getPayments(0, PAYMENT_PAGE_SIZE),
        ]);
      })
      .then(([readingsData, billsData, paymentsData]) => {
        setReadings(readingsData);
        setBills(billsData.content);
        setPayments(paymentsData.content);
        setPaymentPage(paymentsData.number);
        setPaymentTotalPages(Math.max(1, paymentsData.totalPages));
      })
      .catch((requestError: unknown) => {
        if (!(requestError instanceof ApiRequestError && requestError.code === "NOT_FOUND")) {
          setError("Không thể tải dữ liệu. Hãy thử lại sau.");
        }
      })
      .finally(() => setIsLoading(false));
  }, []);

  function updateField(name: keyof RentalContractInput, value: string) {
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submitContract(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    const validationError = validateRentalContract(form);
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSaving(true);
    try {
      setContract(await createRentalContract(form));
    } catch (requestError: unknown) {
      setError(getErrorMessage(requestError, "Không thể lưu hợp đồng."));
    } finally {
      setIsSaving(false);
    }
  }

  async function recordReading(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setReadingError(null);
    const errors = [validatePeriod(readingPeriod), validateMeterValue(readingMeterValue)].filter(Boolean);
    if (errors.length > 0) {
      setReadingError(errors.join(". "));
      return;
    }

    setIsRecordingReading(true);
    try {
      const reading = await recordElectricityReading({ period: readingPeriod, meterValue: readingMeterValue });
      setReadings((current) => [reading, ...current]);
      setReadingMeterValue("");
    } catch (requestError: unknown) {
      setReadingError(getErrorMessage(requestError, "Không thể ghi chỉ số."));
    } finally {
      setIsRecordingReading(false);
    }
  }

  async function createNewBill(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBillError(null);
    const validationError = validatePeriod(billPeriod);
    if (validationError) {
      setBillError(validationError);
      return;
    }

    setIsCreatingBill(true);
    try {
      const bill = await createBill(billPeriod);
      setBills((current) => [bill, ...current]);
    } catch (requestError: unknown) {
      setBillError(getErrorMessage(requestError, "Không thể tạo hóa đơn."));
    } finally {
      setIsCreatingBill(false);
    }
  }

  async function confirmPaymentHandler(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPaymentError(null);
    if (!selectedBillId) {
      setPaymentError("Vui lòng chọn hóa đơn để thanh toán.");
      return;
    }
    if (!paidAt) {
      setPaymentError("Ngày thanh toán là bắt buộc.");
      return;
    }
    // Backend là biên kiểm tra cuối; client chặn sớm ngày tương lai (trước đây
    // là max của input date, giờ dùng DateSelect tự viết).
    const todayIso = new Date().toISOString().split("T")[0];
    const paidAtIso = new Date(paidAt).toISOString().split("T")[0];
    if (paidAtIso > todayIso) {
      setPaymentError("Ngày thanh toán không được ở tương lai.");
      return;
    }

    setIsConfirmingPayment(true);
    const idempotencyKey = paymentIdempotencyKey ?? createIdempotencyKey();
    setPaymentIdempotencyKey(idempotencyKey);
    try {
      const payment = await confirmPayment(
        selectedBillId,
        {
          paidAt: new Date(paidAt).toISOString(),
          note: paymentNote || undefined,
        },
        idempotencyKey,
      );
      setBills((current) => current.map((bill) => bill.id === selectedBillId ? { ...bill, status: "PAID" } : bill));
      setSelectedBillIdState(null);
      setPaidAtState("");
      setPaymentNoteState("");
      setPaymentIdempotencyKey(null);
      void loadPayments(paymentPage, paymentOnTime);
      // Giữ biến để thể hiện response đã được nhận và tránh thay đổi luồng retry.
      void payment;
    } catch (requestError: unknown) {
      // Giữ idempotency key và form values để retry cùng intent an toàn.
      setPaymentError(getErrorMessage(requestError, "Không thể xác nhận thanh toán."));
    } finally {
      setIsConfirmingPayment(false);
    }
  }

  function setSelectedBillId(value: string | null) {
    setSelectedBillIdState(value);
    setPaymentIdempotencyKey(null);
  }

  function setPaidAt(value: string) {
    setPaidAtState(value);
    setPaymentIdempotencyKey(null);
  }

  function setPaymentNote(value: string) {
    setPaymentNoteState(value);
    setPaymentIdempotencyKey(null);
  }

  function setPaymentFilter(value: string) {
    const next = value === "true" ? true : value === "false" ? false : undefined;
    setPaymentOnTime(next);
    void loadPayments(0, next);
  }

  return {
    contract,
    form,
    isLoading,
    isSaving,
    error,
    readings,
    readingPeriod,
    readingMeterValue,
    readingError,
    isRecordingReading,
    bills,
    billPeriod,
    billError,
    isCreatingBill,
    payments,
    paymentPage,
    paymentTotalPages,
    paymentOnTime,
    isLoadingPayments,
    selectedBillId,
    paidAt,
    paymentNote,
    paymentError,
    isConfirmingPayment,
    updateField,
    submitContract,
    recordReading,
    createNewBill,
    confirmPaymentHandler,
    setReadingPeriod,
    setReadingMeterValue,
    setBillPeriod,
    setSelectedBillId,
    setPaidAt,
    setPaymentNote,
    setPaymentPage: (page: number) => void loadPayments(page, paymentOnTime),
    setPaymentFilter,
  };
}

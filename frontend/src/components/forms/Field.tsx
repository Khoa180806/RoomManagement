import type { InputHTMLAttributes } from "react";
import type { RentalContractInput } from "../../features/contracts/api";

export function Field({
  label,
  name,
  helper,
  onChange,
  ...input
}: {
  label: string;
  name: keyof RentalContractInput;
  helper?: string;
  onChange: (name: keyof RentalContractInput, value: string) => void;
} & Omit<InputHTMLAttributes<HTMLInputElement>, "name" | "onChange">) {
  const id = `contract-${name}`;
  return (
    <label className="grid gap-2 text-sm font-semibold text-ink" htmlFor={id}>
      <span>{label} <b aria-hidden="true" className="text-clay">*</b></span>
      <input className="min-h-12 rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus" id={id} name={name} required onChange={(event) => onChange(name, event.target.value)} {...input} />
      {helper && <small className="text-xs font-normal text-muted">{helper}</small>}
    </label>
  );
}

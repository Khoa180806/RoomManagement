export function ErrorMessage({ message }: { message: string }) {
  return <p className="border-l-4 border-danger bg-danger-bg p-3 text-sm text-danger-ink" role="alert">{message}</p>;
}

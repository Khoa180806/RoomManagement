export function EmptyState({ message }: { message: string }) {
  return <p className="py-8 text-center text-sm text-muted" role="status">{message}</p>;
}

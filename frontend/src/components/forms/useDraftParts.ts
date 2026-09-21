import { useState } from "react";

/**
 * Giữ lựa chọn từng phần (ngày/tháng/năm) trong khi giá trị controlled
 * chưa hoàn chỉnh, tránh reset dropdown khi người dùng đang chọn.
 */
export function useDraftParts<T>(value: string, parse: (value: string) => T) {
  const [draft, setDraft] = useState(() => parse(value));
  const [draftSource, setDraftSource] = useState(value);
  const parts = draftSource === value ? draft : parse(value);

  function updateDraft(next: T) {
    setDraft(next);
    setDraftSource(value);
  }

  return [parts, updateDraft] as const;
}

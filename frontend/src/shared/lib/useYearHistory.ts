import { useState } from "react";

export type YearPageState<T> = {
  expanded: boolean;
  toggleExpanded: () => void;
  selectedYear: string;
  setSelectedYear: (year: string) => void;
  years: string[];
  pageItems: T[];
  safePage: number;
  totalPages: number;
  setCurrentPage: (page: number) => void;
};

/**
 * Dùng chung cho các danh sách lịch sử: thu gọn hiển thị `collapsedSize`
 * phần tử gần nhất, mở rộng cho phép lọc theo năm và phân trang
 * `pageSize` phần tử mỗi trang.
 */
export function useYearHistory<T>(
  items: T[],
  yearOf: (item: T) => string,
  options: { collapsedSize?: number; pageSize?: number } = {},
): YearPageState<T> {
  const { collapsedSize = 3, pageSize = 6 } = options;
  const [expanded, setExpanded] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [selectedYear, setSelectedYear] = useState("");

  const itemsByYear = items.reduce<Record<string, T[]>>((groups, item) => {
    const year = yearOf(item);
    if (!groups[year]) groups[year] = [];
    groups[year].push(item);
    return groups;
  }, {});
  const years = Object.keys(itemsByYear).sort((a, b) => b.localeCompare(a));
  const filteredItems = expanded && selectedYear ? itemsByYear[selectedYear] ?? [] : items;
  const totalPages = Math.max(1, Math.ceil(filteredItems.length / pageSize));
  const safePage = Math.min(currentPage, totalPages - 1);
  const pageItems = expanded
    ? filteredItems.slice(safePage * pageSize, (safePage + 1) * pageSize)
    : items.slice(0, collapsedSize);

  return {
    expanded,
    toggleExpanded: () => {
      setExpanded((value) => !value);
      setCurrentPage(0);
      if (expanded) setSelectedYear("");
    },
    selectedYear,
    setSelectedYear: (year) => {
      setSelectedYear(year);
      setCurrentPage(0);
    },
    years,
    pageItems,
    safePage,
    totalPages,
    setCurrentPage,
  };
}

import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";

afterEach(() => {
  cleanup();
});

/**
 * lucide-react nạp bản React riêng trong jsdom (two-copies problem) gây
 * "Invalid hook call". Icon chỉ là hình minh họa nên thay bằng stub SVG.
 * Khi thêm icon mới vào component, bổ sung tên vào danh sách này.
 */
vi.mock("lucide-react", async () => {
  const React = await import("react");

  const ICON_NAMES = [
    "LayoutDashboard", "ReceiptText", "Wallet", "Settings", "LogOut",
    "CalendarDays", "Bell", "BellRing", "Plus", "Loader2", "LoaderCircle",
    "ChevronDown", "ChevronUp", "ChevronLeft", "ChevronRight", "X", "Check",
    "CheckCircle2", "XCircle", "AlertCircle", "Trash2", "Pencil", "Eye",
    "ExternalLink", "Upload", "Home", "TrendingUp", "TrendingDown", "Zap",
    "Droplets", "Wrench", "FileText", "RefreshCw", "ArrowLeft", "ArrowRight",
  ];

  const icons = Object.fromEntries(
    ICON_NAMES.map((name) => {
      const Icon = React.forwardRef(function Icon(
        props: { className?: string },
        ref,
      ) {
        return React.createElement("svg", {
          ref,
          "data-testid": `icon-${name}`,
          className: props?.className,
          "aria-hidden": "true",
        });
      });
      Icon.displayName = name;
      return [name, Icon];
    }),
  );

  return icons;
});

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Pagination } from "./Pagination";

describe("Pagination", () => {
  it("disables the first button on the first page and moves forward", async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();

    render(<Pagination currentPage={0} totalPages={2} onPageChange={onPageChange} />);

    expect(screen.getByRole("button", { name: "← Trước" })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: "Sau →" }));
    expect(onPageChange).toHaveBeenCalledWith(1);
  });
});

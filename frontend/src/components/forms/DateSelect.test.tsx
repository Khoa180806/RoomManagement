import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { DateSelect } from "./DateSelect";

describe("DateSelect", () => {
  it("keeps year and month selections while the controlled value is incomplete", async () => {
    const user = userEvent.setup();

    function Harness() {
      const [value, setValue] = useState("");
      return <DateSelect label="Ngày bắt đầu" value={value} onChange={setValue} />;
    }

    render(<Harness />);
    await user.selectOptions(screen.getByLabelText("Ngày bắt đầu - Năm"), "2025");
    await user.selectOptions(screen.getByLabelText("Ngày bắt đầu - Tháng"), "02");

    expect(screen.getByLabelText("Ngày bắt đầu - Năm")).toHaveValue("2025");
    expect(screen.getByLabelText("Ngày bắt đầu - Tháng")).toHaveValue("02");
    expect(screen.getByLabelText("Ngày bắt đầu - Ngày")).toHaveValue("");

    await user.selectOptions(screen.getByLabelText("Ngày bắt đầu - Ngày"), "02");
    expect(screen.getByLabelText("Ngày bắt đầu - Năm")).toHaveValue("2025");
    expect(screen.getByLabelText("Ngày bắt đầu - Tháng")).toHaveValue("02");
    expect(screen.getByLabelText("Ngày bắt đầu - Ngày")).toHaveValue("02");
  });
});

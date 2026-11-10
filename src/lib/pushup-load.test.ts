import { describe, expect, it } from "vitest";
import { pushUpLoadKg } from "./pushup-load";

describe("pushUpLoadKg", () => {
  it("uses 64% of bodyweight for a regular push-up", () => {
    expect(pushUpLoadKg(74, 0, "regular")).toBe(47.4);
  });

  it("counts a vest at the same share", () => {
    expect(pushUpLoadKg(74, 10, "regular")).toBe(53.8);
  });

  it("is lighter for knee push-ups and heavier with feet raised", () => {
    expect(pushUpLoadKg(80, 0, "knees")).toBeLessThan(pushUpLoadKg(80, 0, "regular"));
    expect(pushUpLoadKg(80, 0, "feet-61")).toBeGreaterThan(pushUpLoadKg(80, 0, "regular"));
  });

  it("clamps values outside the allowed range", () => {
    expect(pushUpLoadKg(10, -5, "regular")).toBe(pushUpLoadKg(35, 0, "regular"));
    expect(pushUpLoadKg(500, 500, "regular")).toBe(pushUpLoadKg(160, 40, "regular"));
  });
});

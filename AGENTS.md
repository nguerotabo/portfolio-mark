# Agent instructions

`plan.md` is the source of truth for scope, architecture, and workflow. Read it before any product change.

1. Stay inside the January scope in `plan.md`. Replacement replay and an assistant are out. Northrail is not imported. The page does not calculate. Prices are not fetched live. Follow the Learning section: do not write the replay, the sell fraction, the CSV rules, the expected cents, or the tradeoff prose unless the user explicitly asks for that named piece.
2. After `spec/ANALYTICS.md` exists, formulas and expected cents live there. Code that disagrees with the spec is wrong. A spec that adds scope `plan.md` does not allow is wrong, and the spec is cut back.
3. A rule change updates `plan.md` and `spec/ANALYTICS.md` in the same change, before the code. Recompute the affected `expected.txt` by hand.
4. Do not edit an expected cent to match an unverified result.
5. The early Python sketch and `demo.html` were removed. Do not restore them. `README.md` only points at `plan.md`.

```text
# Wrong: change the Java replay, then adjust expected.txt until compare is green.
# Right: recompute the cents from spec/ANALYTICS.md, update expected.txt, then fix the engine.
```

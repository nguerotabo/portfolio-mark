# Exact fractions instead of `BigDecimal`

## Context

A lot's value is shares times a close, or dollars divided by a fund close and then times another close. The division does not always terminate: $210 at a close of 55 buys 42/11 fund shares. The spec rounds each lot's value once, to whole cents, half to even. A value that lands exactly on half a cent, like the `half-cent` lot at $0.125, has to round to even, and a value a hair above or below half a cent has to round the other way. The replay needs to know which of those it has.

## Decision

Path values are a `Rational` record: a numerator and a denominator, both `BigInteger`, kept reduced with a positive denominator. Shares, prices, and cents are read from their printed text into exact fractions. Multiplying and dividing never round. The only rounding is `toCents()`, which turns a lot's value in dollars into whole cents, half to even, once per lot per path. Every total, step, and impact is a sum of those cents in a `long`.

`BigDecimal` is used only to read printed text like `185.8904`, which is exact. No path calculation runs in it.

## Alternatives

`BigDecimal` with a fixed precision lost. A division has to stop at some scale, so 42/11 gets cut off, and the cut can push a value that was below half a cent onto it, or one that was exactly on it off it. Then half to even picks the wrong cent, and the result depends on the chosen scale. Python's `Fraction` never does this, so the two engines could disagree.

A `double` lost. It cannot hold 0.1 exactly, so even the printed prices would be wrong before any calculation.

Whole cents in a `long` from the start lost. A lot's value is a fraction of a cent until it is rounded, and rounding earlier, at the fund shares or the price, would round more than once.

## Cost

Numerators and denominators can grow with every multiply and divide, so each operation is slower than fixed-precision arithmetic and needs a reduction. The replay has to call `toCents()` at the right place, once per lot per path. A second call anywhere would round twice, and the type cannot stop that.

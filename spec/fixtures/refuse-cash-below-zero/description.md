The file has a deposit, so the running sum of `net_cash_amount` is checked at the end of every day. Times are empty. Currency is CAD. Account id is DEMO. Commission is 0 on the trade. The deposit quantity copies the cash and is not a share count.

Fund is FUND. As-of date is 2026-06-02. Closes that day are 10.

The buy's printed cash matches 20 shares at 10. At the end of 2026-06-01 the running sum is 100. At the end of 2026-06-02 the running sum is -100 dollars.

Rows:

- 2026-06-01: MoneyMovement EFT, no symbol, quantity 100.0000, cash 100.
- 2026-06-02: Trade BUY Stock A, 20 shares at 10, cash -200.

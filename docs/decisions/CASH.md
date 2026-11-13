# The broker's cash is the truth

## Context

Wealthsimple prints `quantity` and `unit_price` to four decimals, and it also prints `net_cash_amount`. A buy's printed shares times printed price plus commission, and a sale's printed shares times printed price minus commission, do not always come back to the printed cash. The replay has to choose which number is the cash that moved.

## Decision

Cash is always `net_cash_amount`. It is stored as that number times 100, in signed cents. A buy is negative. The product of shares and price is only a check, and only on a trade.

A buy's expected cash is −(shares × price + commission). A sale's expected cash is shares × price − commission. The gap is the absolute difference between `net_cash_amount` and that expected cash. The gap and the tolerance are both in dollars, before the multiplication by 100. The tolerance is `0.00005 × price` plus `0.00005 × shares` plus half a cent, and half a cent is `$0.005`. Inside that sum, the row is accepted and the broker's number is what is stored. Outside it, the run refuses and names the row and the gap.

Deposits, dividends, interest, and tax have no shares-times-price check. Their cash is `net_cash_amount` too.

The `cash-check` fixture is the case that shows a real trade whose printed product disagrees with the printed cash and is still accepted: 2.6897 shares at 185.8904 with cash −499.99. On the real export, every trade fell inside this tolerance, and the worst gap was under half a cent.

## Alternatives

Recomputing the cash as shares × price ± commission, and storing that product, lost. The broker's number is what moved. The product is a four-decimal print, and it can disagree by a fraction of a cent on a trade that really happened.

Refusing any disagreement at all lost for the same reason. The `cash-check` row would refuse a trade the file actually contains.

## Cost

A wrong broker number that still falls inside the tolerance is stored, and it shows up in the account. A gap outside the tolerance refuses the whole run. There is no partial report.

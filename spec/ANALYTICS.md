# Analytics

Portfolio Mark reads a case: an activities file, a price file, a distributions file, and a `run.json` with a fund and an as-of date. It answers the bridge and the pick impacts. Replacement replay and an assistant are out of this version.

# Inputs

A run reads the files in the case folder.

`activities.csv` uses this Wealthsimple header:

`effective_date,effective_time,settlement_date,account_id,account_type,activity_type,activity_sub_type,description,direction,symbol,name,currency,quantity,unit_price,commission,net_cash_amount`

`effective_date,effective_time,account_id,activity_type,activity_sub_type,symbol,currency,quantity,unit_price,commission,net_cash_amount` are read. The rest of the fields are ignored.

`prices.csv` is `symbol,date,close`, with raw closes. An adjusted close already folds in distributions, and the fund paths add distributions separately, so the file holds raw closes.

`distributions.csv` is `symbol,ex_date,amount_per_share`, for the comparison funds. The file is present when a fund pays one.

`run.json` holds `asOf` and `fund`.

One account and one currency per run. The first row in sort order sets the account and the currency. A later row with a different account id, or a different currency, refuses. The demo is Canadian dollars. The account id in a committed fixture is `DEMO`.

# Case

A case is a folder: `activities.csv`, `prices.csv`, `distributions.csv` when a fund pays one, and `run.json`, which holds the comparison fund and the as-of date, for example `{"asOf": "2021-12-01", "fund": "FUND"}`. A fixture is a case plus `expected.json`. Java, Python, compare, the generator, and the page all use this one format. Without `run.json`, a case only reads: the report holds `activities` and `warnings`, or a reader refusal. The page uses that to show the transaction table before a fund is chosen. If a fund pays nothing, `distributions.csv` is not present in the folder. 

The as-of date is never earlier than the last activity. The fund is a symbol the person picks. There is no default fund.

# Normalized row

The normalized row is date, time, type, symbol, currency, quantity, unit price, cash, and the source row number. Cash is `net_cash_amount` in signed whole cents. A buy is negative. `net_cash_amount` must be multipled by 100 to convert it from dollars to cents for precision.  Symbol is empty on a pure cash row. A deposit, a symbol-less interest row, and a symbol-less tax row are pure cash rows. A dividend that names a symbol is not one. The reading rules decide which of those rows is a deposit, interest, tax, or a withdrawal. The source row is the CSV line number. The header is line 1, so the first activity is line 2.

On a `MoneyMovement` row the quantity column is not a share count. The share count is zero. Cash is `net_cash_amount`.

# Reading the file

The real export uses these types and sub-types. `Trade` plus `BUY` is a buy.`Trade` plus `SELL` is a sale. `MoneyMovement` plus `EFT` with positive cash is a deposit. Negative cash is a withdrawal, and this version refuses it. `Dividend` plus `-` is a dividend. `Interest` plus `-` is interest. `Tax` plus `NRT` is a non-resident tax. On dividend, interest, and tax rows the quantity column copies the cash and is ignored. Any other type, or any sub-type not listed here, refuses the run, and the error names the type and the sub-type. A blank or unknown `activity_sub_type` causes a `type` refusal. 

Every number is parsed from its text into an exact value. `185.8904` never passes through a float or a double.

The trade date is `effective_date`. Ignore `settlement_date`. Ignore an executed-at phrase in the description.

Sort by `effective_date`, then `effective_time`. A row with no time sorts before the timed rows on its date. Rows with the same timestamp keep file order. Lots are not merged. An empty time is placed on at the top of the of the rows (sorted by `effective_date`)

Cash is always `net_cash_amount`. It is what moved, so it is never recomputed.  In the case of a `Trade` plus `BUY`, the buy's cash is (shares times price plus commission) * negative, and a sale's cash should equal shares times price minus commission. Quantities and prices both print to four decimals, so the tolerance is `0.00005 × price` plus `0.00005 × shares` plus half a cent. The tolerance and the gap are both in dollars, the CSV's unit, before `net_cash_amount` is multiplied by 100. The gap is the absolute difference between `net_cash_amount` and the expected cash. Half a cent in that sum is `$0.005`. Every trade in the real export falls inside that tolerance, and the worst gap is under half a cent. Outside the tolerance, the run refuses.

When the file has deposits, your cash, the running sum of `net_cash_amount`, must be zero or more at the end of every day. Below zero means the file is missing an earlier deposit or transfer, and the run refuses. The real export's whole file does this on one day. The slice in `exports/activities-slice.csv` stops before that day, and that slice is the real export's later run. This is a note about that one file. A run reads the case folder it was given.

# Money

Shares and prices stay exactly as printed. `3.9152` stays `3.9152`. Path values stay exact fractions and are not rounded along the way. In Java that is a small rational type over `BigInteger`. A `BigDecimal` with a fixed precision can land a hair off an exact half cent and round the wrong way, which Python's `Fraction` never does.

A lot's value on a path is its shares still held, at the as-of close, plus its share of sale proceeds, plus, on a fund path, the distributions it was paid. On your path, a sale's cash is split across that symbol's lots in proportion to the shares each lot sold. A lot's value stays exact until it is rounded once, to whole cents, half to even. `12.5¢` becomes `12¢`. `13.5¢` becomes `14¢`. `-12.5¢` becomes `-12¢`. `-13.5¢` becomes `-14¢`.

Every total, step, and impact is a sum of those cents plus cash that was already whole cents. The impacts sum to stock selection exactly, and the bridge adds up exactly. The account total can sit up to half a cent per lot away from the exact value. That is accepted.

# Dates and prices

A fund purchase or sale uses the fund's close that day, or the next close within 7 days (a close on the 7th day counts). A close on the 8th day is marked as a `missing-close` refusal. A deposit on Sunday 14 June 2026 buys at the Monday 15 June close. If that next close falls after the as-of date, the trade uses the valuation close instead, so the money neither gains nor loses. A stock still held uses the close on the as-of date, or the last close in the 7 days before it. A valuation close is never taken from a day after the as-of date. 

Valuation uses the close on the as-of date, or the last close in the 7 days before it. The as-of date is an input.

A close the replay needs that is not within those 7 days counts as missing. A missing close refuses the run and names the symbol and the date it was needed for. No close is invented, and no close further away is used.

Each downloaded price file is saved once, with a `SOURCES.md` beside it that says where it came from and when.

A close more than 20% away from the same symbol's previous close prints a warning, when that previous close is at most 7 days earlier. Hand-made fixture prices are often months apart, and the 7-day limit keeps them quiet. A warning does not stop the run. Splits are not supported. A split row is an unknown type and refuses.

# Paths

All four paths are valued on the as-of date.

1. **All on day one.** Every contribution dollar buys the fund on the first contribution date and is held.
2. **Fund on your dates.** Each contribution buys the fund on its own date and is held.
3. **Benchmark.** Each purchase's dollars buy the fund that day. Each sale sells the same fraction of the matching fund shares. Proceeds are cash.
4. **Your account.** What happened: shares from the CSV valued at the as-of close, plus all cash.

The three steps are the gaps between neighbouring paths, so the steps add up.

1. **When the money arrived.** Fund on your dates minus all on day one.
2. **Cash and sales.** Benchmark minus fund on your dates.
3. **Stock selection.** Your account minus the benchmark. This equals the sum of the pick impacts.

The headline is cash and sales plus stock selection: your choices versus the fund on your dates. A pick impact is one holding's real path minus the same dollars in the fund.

The bridge is the three steps. All on day one plus the three steps equals your account.

Contributions are the deposits when the file has any. Otherwise each purchase is a contribution on its own date.

Each contribution is its own fund lot on both contribution paths. All on day one buys every one of those lots on the first contribution date. It treats later cash as if it had already existed then.

Neither contribution path sells because a stock was sold.

Each purchase is its own lot on your path and its own fund lot on the benchmark.

Your path and your account are the same path: what happened. A symbol's real path is that symbol's lots and dividends inside your account. 

Every path starts with no cash, and a contribution adds its cash on its date. With no deposits, each purchase is its own contribution, so it brings exactly the cash it spends, even when an earlier sale's proceeds could have paid for it. A path's value is its lots' cents plus its cash outside any lot: the contributions, minus what its purchases spent, plus interest and tax, plus, on your account, the dividends. Sale proceeds stay inside the lot that sold, so they are never counted twice. That is why the cash outside any lot can go below zero when proceeds pay for a later purchase, while your cash in the refusal rule above cannot.

A sale's fraction is the shares sold divided by the shares held just before the sale, in sort order. Every lot of that symbol sells that fraction, and so does every fund lot that came from it, each at its own price. Proceeds are cash and earn nothing. Selling more shares than are held refuses the run. A sale never consumes one chosen lot.

A pick impact is one symbol's real path minus its fund path. The real path is that symbol's lots and dividends. The fund path is the same dollars on the same dates, with the same sale fractions, plus the distributions those fund lots were paid.

A symbol's dividends from the CSV are cash on its real path and one trace line under that symbol, outside any lot. That trace line is the pick's `dividends` integer. The report has no separate trace object.

Every fund path is paid the fund's distributions, and neither side reinvests. For each ex-date on or before the as-of date, a fund lot gets its shares held at the end of the day before the ex-date, times the amount, as cash, inside that lot's single rounding. Your account is not a fund path. When the shares it holds are the comparison fund, those shares are paid that same distribution, which is why a self-holding impact is zero.

Interest and tax land on every path, whether or not the row names a symbol, so they never move a step. That cash stays out of every pick. A pick is the symbol's lots plus its dividends.

Deposited cash that is never spent stays cash on your account and the benchmark. The contribution paths invest all of it, so it shows up in cash and sales.

When sale proceeds pay for a later purchase, the benchmark's cash can go below zero. That is allowed. The benchmark is a measurement, not a portfolio you could have held.

Holding the comparison fund itself has an impact of zero, apart from the gap between the fill and that day's close and the cent rounding of the trade's cash. The fill is the trade's `unit_price`. The gap is that price minus the fund's close on the trade date.

`bestPick` is the most positive impact. `largestDrag` is the most negative. The report says none when no impact has that sign. None is `null`. Ties go to the symbol that sorts first in plain ASCII order.

# Worked example

Made-up history. No deposits, so each purchase is a contribution on its date. No dividend and no distribution. Trade prices come from the CSV. Closes come from the price file. The dates are in 2021, when all four dates were weekdays.

Fund closes: 15 Jan $100, 1 Mar $125, 1 Jun $120, 1 Dec $150.

- 15 Jan: spend $4,000 on Stock A at $40, which is 100 shares. The same $4,000 buys 40 fund shares.
- 1 Mar: spend $2,000 on Stock B at $20, which is 100 shares. The same $2,000 buys 16 fund shares.
- 1 Jun: sell 50 shares of Stock A at $60, which sets aside $3,000. That is half, so the benchmark sells 20 of the 40 fund shares at $120, which sets aside $2,400. Stock B is not sold.
- 1 Dec is the as-of date. Stock A is $70. Stock B is $25. The fund is $150.

| Path | Value on 1 Dec |
| --- | --- |
| All on day one | $6,000 buys 60 fund shares at $100. 60 × $150 = $9,000 |
| Fund on your dates | 40 + 16 = 56 fund shares. 56 × $150 = $8,400 |
| Benchmark | The 20 fund shares still held from Stock A are $3,000, plus $2,400 of cash, plus the 16 from Stock B at $2,400. Total $7,800 |
| Your account | 50 × $70 = $3,500, plus $3,000 of cash, plus 100 × $25 = $2,500. Total $9,000 |

| Line | Arithmetic | Result |
| --- | --- | --- |
| When the money arrived | $8,400 minus $9,000 | a loss of $600 |
| Cash and sales | $7,800 minus $8,400 | a loss of $600 |
| Stock selection | $9,000 minus $7,800 | a gain of $1,200 |
| Headline | cash and sales plus stock selection | a gain of $600 |

Stock A ends at $6,500 against a fund path of $5,400: a gain of $1,100. Stock B ends at $2,500 against $2,400: a gain of $100. Together they are the $1,200 of stock selection.

The second $2,000 went in on 1 March, after the fund had risen from $100 to $125, which cost $600. Selling half of Stock A on 1 June cost another $600 measured in the fund, because the 20 fund shares sold at $120 would have been worth $150 each. Picking the two stocks instead of the fund earned $1,200.

Small sale, fund path only. $4,000 when the fund is $20 buys 200 shares. Selling 100 when the fund is $40 sets aside $4,000. The other 100, at $40 on the as-of date, are worth $4,000. The path ends at $8,000. The pick is the real ending wealth minus $8,000.

Self-holding check. $1,000 of the comparison fund, filled at the $50 close, is 20 shares. The fund pays $0.50 a share and then closes at $55. The ex-date is after the buy and on or before the as-of date, so the 20 shares were held at the end of the day before the ex-date. Your path is 20 × $55 plus the $10 distribution, which is $1,110. The fund path is the same $1,110. The impact is zero.

# Refusals

A refusal stops the whole run and returns one error: a code, the fields its row names, and a message for people. There is no partial report. Each code is its fixture's name without `refuse-`, for example `missing-close`. When two rules fail, the run returns the refusal it hits first while walking the rows in sort order, and then stops.

| Refusal | Code | The error names |
| --- | --- | --- |
| An unknown or missing column | `column` | The column |
| A second currency | `currency` | The currency and the row |
| A second account | `account` | The row |
| An unknown type or sub-type | `type` | The type, the sub-type, and the row |
| A withdrawal | `withdrawal` | The row |
| Cash outside the tolerance | `cash` | The row and the size of the gap |
| Cash below zero, when the file has deposits | `cash-below-zero` | The date and the shortfall |
| An as-of date before the last activity | `as-of` | Both dates |
| A sale larger than the shares held | `oversell` | The symbol and the date |
| A missing close | `missing-close` | The symbol and the date it was needed for |

`fields` is an object. Its keys for each code:

| Code | Keys |
| --- | --- |
| `column` | `column` |
| `currency` | `currency`, `row` |
| `account` | `row` |
| `type` | `type`, `subType`, `row` |
| `withdrawal` | `row` |
| `cash` | `row`, `gap` |
| `cash-below-zero` | `date`, `shortfall` |
| `as-of` | `asOf`, `lastActivity` |
| `oversell` | `symbol`, `date` |
| `missing-close` | `symbol`, `date` |
| `price-jump` | `symbol`, `date`, `previousDate` |

## Warnings 

`warnings` is an array. Each warning has `code`, `fields`, and `message`. A warning does not stop the run.

| Warning | Code | The warning names |
| --- | --- | --- |
| A close more than 20% from the previous close, at most 7 days earlier | `price-jump` | The symbol and the two dates |


# Report

Money fields are integer cents. Quantities and prices are strings, exactly as the CSV printed them, never JSON numbers. Dates are `YYYY-MM-DD`. Activities keep their sort order. Picks go by symbol in ASCII order, and lots by purchase. None is `null`.

`asOf` is a date. `fund` is a string.

`paths` holds `allOnDayOne`, `fundOnYourDates`, `benchmark`, and `account`, each an integer number of cents.

`steps` holds `whenMoneyArrived`, `cashAndSales`, and `stockSelection`, each an integer number of cents.

`headline` is an integer number of cents.

`picks` is an array. Each pick has `symbol` (string), `impact` (integer cents), `dividends` (integer cents), and `lots`. Each lot has `account` and `fund`, the lot's value on your path and on its fund path, in integer cents.

`bestPick` and `largestDrag` are a symbol string, or `null` when no impact has that sign.

`activities` is an array of normalized rows: `date`, `time` (string or `null`), `type`, `symbol` (string or `null`), `currency`, `quantity` (string or `null`), `unitPrice` (string or `null`), `cash` (integer cents), and `sourceRow` (integer).

the following are the `type` options. `buy` for a buy, `sale` for a sale, `deposit` for a deposit, `dividend` for a dividend, `interest` for interest, and `tax` for tax.

`warnings` is an array. Each warning has `code`, `fields`, and `message`. A warning does not stop the run.

`error` is present instead of the rest when the run refuses. It has `code`, `fields`, and `message`. The message is for people. Compare checks the code and the fields, not the wording.

An `expected.json` holds only the fields its case checks, in this shape.

This is the worked example, in cents. Both picks are gains, so `largestDrag` is `null`. There is no warning.

```json
{
  "asOf": "2021-12-01",
  "fund": "FUND",
  "paths": {
    "allOnDayOne": 900000,
    "fundOnYourDates": 840000,
    "benchmark": 780000,
    "account": 900000
  },
  "steps": {
    "whenMoneyArrived": -60000,
    "cashAndSales": -60000,
    "stockSelection": 120000
  },
  "headline": 60000,
  "picks": [
    {
      "symbol": "Stock A",
      "impact": 110000,
      "dividends": 0,
      "lots": [{ "account": 650000, "fund": 540000 }]
    },
    {
      "symbol": "Stock B",
      "impact": 10000,
      "dividends": 0,
      "lots": [{ "account": 250000, "fund": 240000 }]
    }
  ],
  "bestPick": "Stock A",
  "largestDrag": null,
  "activities": [
    {
      "date": "2021-01-15",
      "time": null,
      "type": "buy",
      "symbol": "Stock A",
      "currency": "CAD",
      "quantity": "100",
      "unitPrice": "40",
      "cash": -400000,
      "sourceRow": 2
    },
    {
      "date": "2021-03-01",
      "time": null,
      "type": "buy",
      "symbol": "Stock B",
      "currency": "CAD",
      "quantity": "100",
      "unitPrice": "20",
      "cash": -200000,
      "sourceRow": 3
    },
    {
      "date": "2021-06-01",
      "time": null,
      "type": "sale",
      "symbol": "Stock A",
      "currency": "CAD",
      "quantity": "50",
      "unitPrice": "60",
      "cash": 300000,
      "sourceRow": 4
    }
  ],
  "warnings": []
}
```

A missing close returns only the error:

```json
{
  "error": {
    "code": "missing-close",
    "fields": { "symbol": "FUND", "date": "2021-01-15" },
    "message": "No close for FUND within 7 days of 2021-01-15."
  }
}
```

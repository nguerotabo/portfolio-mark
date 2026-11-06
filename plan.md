# Portfolio Mark

This file is the source of truth for scope, architecture, and workflow. Read it before changing the product.

`spec/ANALYTICS.md` does not exist yet. Create it in the week of 9 November 2026. After that, it is the contract for formulas and expected cents. Java and Python are wrong when they disagree with it. If the spec takes on scope this file does not allow, this file wins and the spec is cut back. A rule change updates this file and `spec/ANALYTICS.md` in the same change, before the code.

The early Python sketch and `demo.html` have been removed. Do not restore them. Do not treat QQC as a built-in fund. `README.md` only points here.

Ready by 31 January 2027. Interviews are most likely February and March 2027. One person. About 10 to 20 hours a week.

## What it is

A person uploads one Wealthsimple activities CSV. The page lists every row. They choose one comparison fund. Three answers appear:

1. **Pick impact.** One holding versus the same dollars in the comparison fund.
2. **Portfolio benchmark.** Every purchase replayed into that fund. The gap equals the sum of the pick impacts.
3. **Contribution timing.** Both sides hold only the comparison fund. Real contribution dates versus all of those dollars on the first contribution date. Not added to the pick gap.

There is one page and no Next button. The page does not calculate. Java writes a report. The page shows it.

## What this version is not

- Not a second Wealthsimple download. The holdings report is not an input.
- Not a live balance. The number will not match the Wealthsimple app. The page says so.
- Not a live price feed. Closes are copied by hand into a price file before the demo.
- Not Northrail. Do not import its schema, Kotlin services, or ops screens. An entry there is an account, cents, a transfer, and a time. It has no symbol, shares, or price.
- Not replacement replay. Swapping one named stock for another named stock is stretch, after the three answers are green.
- Not an assistant. An assistant may later explain a report the two engines already matched. It does not compute. It is not on the January page.
- Not a database, not Spring, not CodeQL.

## Stack

| Layer | Choice |
| --- | --- |
| Engine | Java 21, Gradle, JUnit 5. Package `mark`. Serves the page. |
| Check | Python 3 in `python/verifier/`. Same fixtures. Never serves the page. Never imports Java. |
| Page | React and TypeScript. Next.js is allowed. No formula in the page. |
| History | The normalized activity file produced from the CSV. |
| Prices | `prices.csv`, typed by hand. About eight comparison funds plus the symbols in the fixture. |
| CI | A GitHub Action that labels a pull request `money-path` when the diff touches the math, the price reader, rounding, or the report. A markdown-only diff gets no label. |

## Architecture

One engine decides. The page asks that engine for a report and displays it. A second program checks the same fixtures and never sits on the request path. There is no database. The source of truth for a run is the normalized activity list both engines read.

```text
activities CSV --> normalized activities --+--> Java engine --> report --> page
price file --------------------------------+
                                              Python verifier reads the same fixtures
                                              and must agree. It never serves the page.
```

The comparison-fund replay inside Java produces two of the three answers. Pick impact is that replay aimed at one holding. The portfolio benchmark is that replay aimed at every purchase. Timing is a second calculation beside it: both sides hold only the fund, on two schedules. Swapping an arbitrary pair of symbols is the same replay pointed at a pair the person names, and that control is not in this version.

A run needs three inputs: the activities, the price file, and the comparison symbol. The report carries the as-of date, the actual value, the alternative value, the gap, the per-symbol breakdown, and a trace row for each source transaction (the cash it contributed and its share of the gap). Portfolio state after the run is cash plus the shares still held.

| Path | What it holds |
| --- | --- |
| `spec/ANALYTICS.md` | The contract: formulas, rows, currency, edge cases, expected outputs |
| `spec/fixtures/<case>/` | Activities, prices, and the expected report for one case |
| `java/src/main/java/mark/ledger/` | The normalized activity, and the reader that builds it from a CSV |
| `java/src/main/java/mark/market/` | Closes by symbol and date |
| `java/src/main/java/mark/analytics/` | Replay, timing, and the report |
| `java/src/test/java/mark/` | One test that runs every fixture and compares the report |
| `python/verifier/` | A separate implementation. Same fixtures. Same reports. No import of the Java code |
| `web/` | The decision page. Built after the two engines match. It asks Java for the report |
| `scripts/compare` | Runs Java and Python on every fixture and fails when a field disagrees |
| `.github/workflows/` | The money-path label. It reads the pull request diff. It is not on the request path |

Do not create these by copying the retired root sketch. Java is a command-line program: it reads the activities and the price file and writes the report. The page shells out to that program. No Spring, no database, no live price feed.

## Workflows

### Changing a rule

1. Change this file if the scope or the workflow changed.
2. Change `spec/ANALYTICS.md` if a formula, a refusal, or an expected cent changed.
3. Recompute the affected `expected.txt` by hand.
4. Then change Java, then Python.
5. `scripts/compare` must be green before the change is done.
6. Do not edit an expected cent to match a result you have not recomputed on paper.

### Fixtures

Each fixture is a folder under `spec/fixtures/<name>/`.

- `activities.csv` uses the Wealthsimple header: `effective_date,effective_time,settlement_date,account_id,account_type,activity_type,activity_sub_type,description,direction,symbol,name,currency,quantity,unit_price,commission,net_cash_amount`.
- Account id in committed fixtures is `DEMO`. Do not commit a real account id.
- `prices.csv` is `symbol,date,close`. Only the dates the fixture uses.
- `expected.txt` is labeled whole cents: `actual`, `benchmark`, `selection`, `timing`, and one line per symbol impact. A refusal fixture has one `error` line that names the symbol, the date, or the activity type.

Minimum set: buys-only, pick-impact, benchmark, timing, one sell, missing close, second currency, cash off by two cents, sell larger than the shares held, deposit-quantity, nearest-cent.

### Engines

Java is the product. Python is a second implementation of the spec, written without reading the Java replay methods. `scripts/compare` runs both and prints the fixture name and the field on a mismatch. Keep one planted-bug check: a changed expected cent must make compare fail, then restore it.

Commit when the fixture you touched is green. The message names the fixture.

### Page

The page runs Java and displays the report. Transaction table first. Answer blocks stay empty until a comparison fund is chosen. Opening a symbol lists the lots under its impact. A saved report is only a fallback when Java cannot serve the page, and the README says that.

### Pull requests

The money-path label is a file-path check, not CodeQL, and not an assistant. It fires when the diff touches `java/src/main/java/mark/`, the price reader, rounding, or the report fields. It does not prove a dollar is correct. The fixtures do that.

### Rollback

| If this fails | Fall back to |
| --- | --- |
| A week goes red | The last commit where every fixture matched. Do not patch forward on a red main. |
| The real CSV reader slips | Demo the hand-computed fixture. Show the real file only as far as it parses. |
| Sells are still red | Demo the buys-only file. The sell rule stays in the spec. Say that fixture is open. |
| Python disagrees | Do not call the page verified. Show the case and the field. |
| Java cannot serve the page | The page reads the last report saved from a green run. |
| A price is missing | Show the refusal with the symbol and the date. Do not fill in a close. |
| Replay is still red in January | Show the spec, the fixtures, and the one fund replay that matches. |

## Domain rules

The trade date is `effective_date`. Ignore `settlement_date`. Ignore an executed-at phrase in the description.

Quantity is shares only on a trade (`Trade` + `BUY` or `SELL`). On a deposit, dividend, interest, or tax row, that column copies the cash and is not a share count.

Share counts stay exact. `3.9152` stays `3.9152`. Unit price stays at the file's precision, such as `185.8904`. Cash is integer cents and is shown to two decimal places. The one-cent check is shares times price against net cash and commission. Within one cent, keep the file's cash and record that a cent was rounded. A wider gap refuses the row.

One currency. One account. A second of either refuses the run. An unknown activity type refuses the run and the message names the type. Map `MoneyMovement` + `EFT` to deposit, `Dividend` to dividend, `Interest` to interest, `Tax` to tax.

Rows with the same timestamp stay in file order. Lots are not merged.

Symbol-less cash (deposit, interest, non-resident tax) is applied to both paths, so it does not create a pick gap. A dividend stays on that holding and inside its pick. The fund path does not invent a dividend. A later buy that spends the dividend cash is its own purchase.

If the file has deposits, timing uses the deposit dates. A buy spends cash already there and is not a second contribution. If the file has buys and no deposits, each buy is a contribution on the buy date. The day-one side treats later cash as if it had existed on the first contribution date. Say that on the page. Timing does not sell because a stock was sold, so it differs from the benchmark once a sale exists, and also when a deposit date is not the buy date.

A sale sells the same fraction of every lot, not one chosen lot. Fraction is shares sold divided by shares held that day. The fund path sells that fraction of the fund shares that came from those lots, at the fund's close on the sale date. Each path uses its own price. Selling more shares than are held refuses the run.

The as-of date is the latest date in the activities file. The close is the last market close on or before that date. A missing close refuses the run. No price is invented.

Pick impact for a symbol is that symbol's ending wealth, including its dividends, minus the fund path of the dollars that bought it. On a buy, fund shares equal cash divided by the fund's close that day. Ending wealth of what is still held uses the as-of close. The pick is the difference of the two ending wealths. It is not buy price minus sale price, and it is not the gain above the original dollars.

The portfolio gap is actual ending value minus the all-in fund replay. It equals the sum of the pick impacts when symbol-less cash sits on both sides and dividends sit inside the picks.

## Worked examples

These numbers go into `spec/ANALYTICS.md` in week 1. A reader must be able to recompute them from that file alone.

Fund closes: 15 Jan $100, 1 Mar $125, 1 Jun $120, 1 Dec $150.

- 15 Jan: spend $4,000 on Stock A at $40 (100 shares).
- 1 Mar: spend $2,000 on Stock B at $20 (100 shares).
- 1 Jun: sell 50 shares of Stock A at $60. Stock B is not sold.
- 1 Dec is the as-of date. Stock A is $70. Stock B is $25.

Stock A ends at $6,500. Its fund path ends at $5,400. Impact +$1,100. Stock B ends at $2,500 against $2,400. Impact +$100. The account is $9,000. The fund path is $7,800. Selection is +$1,200. Timing, with no stock sale mirrored, is $8,400 on the real dates against $9,000 on day one, which is −$600.

Small sale, fund path only. $4,000 when the fund is $20 buys 200 shares. Selling 100 when the fund is $40 sets aside $4,000. The other 100, at $40 on the as-of date, are worth $4,000. The path ends at $8,000. The pick is the real ending wealth minus $8,000.

## Learning with AI

You cannot defend code you have never understood, and if an assistant writes it, you never learn it. The pitch for this product is that an assistant may explain a report and must not compute one. Use the same rule on yourself. An assistant can propose. You are the checker, and the checker has to understand the code.

For every feature: test, struggle, ask.

1. Write the test first, yourself. Before any production code, write the failing test against `expected.txt`. You cannot write it without knowing what the feature must do. This is where most of the learning happens.
2. Struggle for 30 to 45 minutes with no assistant. The spec, this file, the Java docs, a search. Being stuck is the learning.
3. Then ask one narrow question. Not "write the replay." Ask "how do I keep a fractional share exact in Java without turning the cash into a float?" You get one idea, and you still write the code around it.
4. Type everything. Never paste. Even if an answer shows twenty lines, retype them. Typing forces you to read every token.
5. Explain it back. After it works, two sentences in the commit message say why. If you cannot write them, you did not learn it.

Use an assistant for explaining an idea, reading an error you do not understand, syntax you forgot, reviewing code you wrote, and boring scaffolding: Gradle lines, a Next.js table, GitHub Actions YAML. Do not use an assistant for the replay, the sell fraction, the CSV rules, the expected cents, the architecture, or the README tradeoffs. If a manager could ask "why did you do it this way?", you must have written it.

Read the idea, then build it with the reference closed.

| When | Idea | Read, short | Then |
| --- | --- | --- | --- |
| 9 Nov | What a replay is | The worked examples in this file | Write them into the spec in your own sentences |
| 30 Nov | The activities file | The column rules in this file | The reader tests, before the reader |
| 7 Dec | Buys-only replay | The $4,000 at $20 example | Implement it, then delete the method and rewrite it from memory |
| 28 Dec | A sale across lots | The fraction rule in this file | The sell fixture, on paper first |
| 4 Jan | A second implementation | Why Python must not read the Java methods | Write the verifier from the spec |
| 11 Jan | The pull-request label | GitHub's path-filter docs | You choose which paths are money. The YAML may be drafted |

Keep `docs/mistakes.md`. Every time something breaks in a way you did not expect, write three lines: what happened, why, what you changed. That file is the "what broke" answer in the review.

Two checks. After the buys-only replay works, delete that method and rewrite it from memory on a branch. Thirty minutes. And explain a sale out loud: "The same dollars buy the fund on the buy date. A later sale sells the same fraction, at the fund's price that day. The pick is the difference of the two ending values." If you stall, that is the gap.

The sentence you can say, if it is true: "I used an assistant for scaffolding and to explain errors. I wrote every line that decides a cent."

## Weeks

Do the week from the top. The week is done only when its done line is true. If the week has about 10 hours, finish the done line and skip polish.

### 9–15 November — the spec

Create the empty folders listed above. Write `spec/ANALYTICS.md` with the product paragraph, the normalized row, the date rule, the money rule, the three formulas with the worked examples, the path rules, and the refusals. The next morning, recompute +$1,100 and the $8,000 path from that file alone.

Done when that recomputation needs no other document. Do not write Java, a page, or a fixture.

### 16–22 November — cases

Add a case for each of these, with cents or a refusal sentence: deposit quantity 700 is cash not shares; dividend quantity 30.90 is cash not shares; interest of 1 cent and tax of 2 cents do not change the pick gap; four buys at one timestamp stay four lots; a 14 June deposit spent on 22 June makes timing and the benchmark use different dates, with one cent of cash left; a dividend then a buy puts the dividend inside the pick and treats the buy as its own purchase; as-of on a non-trade last row uses the last market close on or before that date; a sale of half applies to every lot; 2.6897 shares at 185.8904 with cash −499.99 is accepted and a two-cent gap is refused; USD, a second account, and an unknown activity type refuse.

Done when no case says "to be decided." Do not start Java.

### 23–29 November — fixtures

Build the fixture set in Workflows, by hand, with account id `DEMO`. Recompute `expected.txt` the next day.

Done when the folders exist and each expected file can be recomputed without running code. Do not change an expected number to hide uncertainty.

### 30 November – 6 December — the reader

Java 21 Gradle project, JUnit 5, package `mark`, no Spring, no database. `mark.ledger` holds an activity plus its source row number. `mark.market` reads closes and fails with the symbol and the date when one is missing. Tests pass for deposit-quantity, nearest-cent, missing close, and second currency.

Done when those four pass and a report line can name its source row. Do not replay yet.

### 7–13 December — buys-only replay

One function replays buys into the comparison fund. No sells. Ending value is shares still held at the as-of close, plus leftover cash. The buys-only fixture matches to the cent.

Done when a second run gives the same cents. Commit names the fixture. Do not start the page.

### 14–20 December — pick impact and the benchmark

Per-symbol impact includes that symbol's dividends. Symbol-less cash is on both totals. A test sums the impacts and compares them to selection. Both fixtures match.

Done when dropping one symbol fails the sum test. Do not build the arbitrary symbol swap.

### 21–27 December — holiday

If any replay fixture is red, fix that and do not start timing. If they are green, add timing in its own field, with the sentence about later cash, and a test that it is not added to selection.

Done when timing matches, or the red replay fixture is fixed instead. No sells and no page.

### 28 December – 3 January — sell, dividend, fee

Implement the fraction rule. Wire the oversell refusal. Dividend fixture: cash on the actual holding only, sum of impacts still holds. Fee fixture: cash falls on both paths, selection unchanged. If the sell fixture is still red on 3 January, leave `expected.txt` alone and write the failure at the top of the spec.

Done when dividend and fee match, and the sell fixture either matches or is an honest written failure.

### 4–10 January — Python

Write the verifier from the spec and the fixtures, not from the Java methods. `scripts/compare` prints the fixture and the field. Every fixture matches. A planted one-cent change makes compare fail. Delete that copy.

Done when compare is green on the real fixtures and you have seen it fail on purpose. Do not start the page.

### 11–17 January — page and label

React and TypeScript page, one screen, three answers, lots under a symbol, Java produces the report. Add the money-path workflow. Change one fixture input and confirm the page moved only because the report moved.

Done when someone else can upload the demo file, pick a fund, and read the three answers. Do not put arithmetic in the page.

### 18–24 January — cold run

Timed eight-minute path from a fresh terminal. Fix only a break in that path. README in the words you will say: how to run Java, how to run compare, the three answers, no live prices, the value will not match the Wealthsimple app, no assistant, no replacement control. Record about 90 seconds of the clicks. Say the four tradeoffs once: nearest cent and say so; the sell rule written before the code; timing kept separate; Java serves the page.

Done when you can do the path without this file open, and the recording exists.

### 25–31 January — freeze

Rehearse the 30 minutes on two different days. On a break, use the rollback table. No new rule, fund, activity type, or price call.

### February and March — reviews

The week of a review, rehearse once and change nothing. If the sell fixture is red, show the spec and say so. If asked about the Wealthsimple balance, say the page uses the price file on the last activities date and will not match the live app. A fix after a review is only for something that failed in the room.

## Demo shape

| Minutes | What you do |
| --- | --- |
| 0–1 | One ending value hides which decisions made it. |
| 1–9 | The file, then the three answers. Open the largest drag. Show timing is not added. Show a price copied by hand, then a pull request that gets the money-path label. |
| 9–14 | CSV, normalized activities, price file, Java report, page. Python checks fixtures and does not serve the page. |
| 14–20 | The four tradeoffs. Stop at four. |
| 20–24 | A real bug, or a fixture the spec refuses. |
| 24–30 | Their questions. |

The line that carries the review: the engine decides, a second engine has to agree, and the page shows the transactions behind the number.

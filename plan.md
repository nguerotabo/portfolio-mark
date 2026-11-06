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

Do the week from the top. Each week has a goal, the steps, a done line, and a stop line. The week is done only when its done line is true. Ten to twenty hours is the budget. If the week has about 10 hours, finish the done line and skip polish.

### 9–15 November — the spec

Write `spec/ANALYTICS.md`, the contract. No application code.

1. Create the empty folders from the architecture table: `spec/fixtures/`, `java/src/main/java/mark/ledger/`, `java/src/main/java/mark/market/`, `java/src/main/java/mark/analytics/`, `java/src/test/java/mark/`, `python/verifier/`, `web/`, `scripts/`, and `.github/workflows/`. Do not copy Northrail into any of them.
2. Create `spec/ANALYTICS.md`. Open it with one paragraph: Portfolio Mark reads one activities file and answers pick impact, the portfolio benchmark, and timing. Replacement replay and an assistant are out of this version.
3. Write the normalized row: date, type, symbol, currency, quantity, unit price, cash. Quantity is a share count only on a trade. On a deposit, dividend, interest, or tax row, that column copies the cash and is not shares.
4. Write the date rule. The trade date is `effective_date`. Ignore `settlement_date`. Ignore an executed-at phrase inside the description.
5. Write the money rule. Share counts stay exact, including `3.9152`. Unit price stays at the file's precision, such as `185.8904`. Cash is whole cents. If shares times price, adjusted for commission, is within one cent of the file's cash, keep the file's cash and note that a cent was rounded. A wider gap refuses the row.
6. Write the three formulas in words and with the made-up numbers, so a reader can recompute them from this file alone. Stock A ends at $6,500 against a fund path of $5,400, impact +$1,100. Stock B is +$100. The account is $9,000 against $7,800, gap +$1,200, which is the sum of the impacts. Timing is $8,400 against $9,000, which is −$600, and it is not added to the gap.
7. Write the small sale. $4,000 when the fund is $20 buys 200 shares. Selling 100 when the fund is $40 sets aside $4,000. The other 100, at $40 on the as-of date, are worth $4,000. The path ends at $8,000. The pick is the real ending wealth minus $8,000, not the $4,000 of growth.
8. Write the path rules. Symbol-less cash, including a deposit, interest, and a non-resident tax, is applied to both paths. A dividend stays on that holding and inside its pick. The fund path does not invent a dividend. If the file has deposits, timing uses the deposit dates. A buy spends cash already there and is not a second contribution.
9. Write the refusals. A second currency, a second account, an unknown activity type, a sale larger than the shares held, and a missing close. Each refusal names what was wrong.
10. Close the file. The next morning, recompute +$1,100 and the $8,000 sale path using only `spec/ANALYTICS.md`. If you need this file to do it, the spec is not done.

Done when `spec/ANALYTICS.md` is the only document a new reader needs to recompute the two examples.

Stop. Do not create a Java class, a React component, or a fixture file this week.

### 16–22 November — cases

Add one expected outcome for every real-file case, in the same spec.

1. Add a Cases section. Each case gets a name, the input in one or two rows, and either the cents that come out or the exact refusal.
2. Case deposit-quantity. A `MoneyMovement` row with quantity 700 and cash 700. Expected: cash of 70000 cents, and zero shares.
3. Case dividend-quantity. A dividend with quantity 30.90 and cash 30.90. Expected: cash on that symbol, not 30.90 shares.
4. Case interest-and-tax. Interest of 1 cent and a non-resident tax of 2 cents, no symbol. Expected: both paths change by the same cash, so the pick gap does not change.
5. Case same-second. Four buys with the same timestamp. Expected: four lots, in file order, not one merged lot.
6. Case deposit-then-buy. $19,260 deposited on 14 June, spent on 22 June, one cent left. Expected: timing buys the fund on 14 June. The benchmark buys the fund on 22 June. The one cent remains cash on both.
7. Case dividend-then-buy. Dividend of $30.90, then a buy of $30.89 the next day. Expected: the dividend is inside that symbol's pick. The buy is a separate purchase. The fund path has no invented dividend.
8. Case as-of. The last row is interest, not a trade. Expected: the valuation day is that date, and the close used is the last market close on or before it.
9. Case sell-fraction. A sale of half the shares, across two lots. Expected: half of each lot, on the real path and on the fund path, each at its own price. A sale larger than the shares held is a refusal.
10. Case nearest-cent. 2.6897 shares at 185.8904 with cash −499.99. Expected: accepted, with a note that a cent was rounded. A copy of the row whose cash is off by two cents is refused.
11. Case boundaries. One USD row refuses the run. A second account id refuses the run. An activity type that is not in the spec refuses the run and the message contains that type.

Done when every case in the Cases section has a number or a refusal sentence. Nothing says "to be decided."

Stop. Do not start the Java reader until those sentences exist. If a case has no honest answer, change the rule in the spec, not later in the code.

### 23–29 November — fixtures

Turn the cases into fixture folders with answers computed by hand.

1. For each fixture, create `spec/fixtures/<name>/` with `activities.csv`, `prices.csv`, and `expected.txt`. The activities file uses the Wealthsimple header: `effective_date,effective_time,settlement_date,account_id,account_type,activity_type,activity_sub_type,description,direction,symbol,name,currency,quantity,unit_price,commission,net_cash_amount`.
2. Use account id `DEMO`. Do not commit a real account id.
3. `prices.csv` is `symbol,date,close`. Put the demo symbols and about eight comparison funds in it, only on the dates the fixtures use. Type the closes yourself. Do not call a market site from a program.
4. `expected.txt` uses labeled whole cents: `actual`, `benchmark`, `selection`, `timing`, and one line per symbol impact. A refusal fixture instead has one line, `error`, containing the symbol, the date, or the activity type.
5. Build buys-only, pick-impact, benchmark, and timing from the same small history, so you can see that the symbol impacts add to selection and that timing is a different pair of totals.
6. Build one sell fixture from the half-the-shares case. Compute both paths on paper before you type `expected.txt`.
7. Build the refusal fixtures: missing close, second currency, cash off by two cents, sell larger than the shares held.
8. Build deposit-quantity and nearest-cent from the real export's shape, with made-up account ids and the amounts from the spec.
9. Check the arithmetic a second time the next day. If the paper total and `expected.txt` disagree, fix the file you typed wrong. If both disagree with the spec, fix the spec first.

Done when every fixture named above has a folder and an expected file you can recompute without running code. That is the minimum set in Workflows: buys-only, pick-impact, benchmark, timing, one sell, missing close, second currency, cash off by two cents, sell larger than the shares held, deposit-quantity, nearest-cent.

Stop. Do not change an expected number because you are unsure. Uncertainty means the spec case is unfinished.

### 30 November – 6 December — the reader

A Java program that reads the fixtures into activities and closes, and does not replay yet.

1. Create a Java 21 Gradle project in `java/`. Use JUnit 5. Do not add Spring, and do not add a database.
2. In `mark.ledger`, write an Activity type with the normalized fields plus the source row number.
3. Write a test that builds three activities by hand and reads them back. No file is involved yet.
4. Write the CSV reader. Unknown column: fail with that column name. Date: `effective_date`. Trade plus BUY is a buy. Trade plus SELL is a sell. `MoneyMovement` plus `EFT` is a deposit. Dividend, Interest, and Tax map to those types. Any other activity type fails, and the message contains the type.
5. On a trade, quantity is shares. On every other type, ignore quantity as a share count and take the cash from `net_cash_amount`.
6. Convert cash to cents. Apply the one-cent check, including commission. Keep a flag on the row when a cent was rounded.
7. Refuse a second currency or a second account id.
8. In `mark.market`, read `prices.csv`. A missing close fails with the symbol and the date. The close for a valuation day is the last close on or before that day.
9. Point the tests at deposit-quantity, nearest-cent, the missing-close refusal, and the second-currency refusal.

Done when those four fixtures pass. A report line can name the source row number it came from.

Stop. Do not write the replay this week. A reader that invents a share count for a deposit is not done.

### 7–13 December — buys-only replay

One buys-only replay into the comparison fund matches its fixture to the cent.

1. In `mark.analytics`, write one function. Input: the activities, the price book, the comparison symbol, the as-of date. Output: shares of the fund bought with each buy's cash, at that day's close.
2. Do not handle sells. Ending value is shares still held times the as-of close, plus leftover cash.
3. Run the buys-only fixture. Compare every labeled cent in `expected.txt`.
4. On a mismatch, decide which side is wrong by recomputing on paper. Change the code if the paper matches the spec. Change the spec, then the expected file, only if the rule itself was wrong.
5. Commit when the fixture is green. The commit message says which fixture passed.

Done when the buys-only expected file matches, and the command can be run again with the same result.

Stop. Do not start pick impact, timing, or the page. Do not edit `expected.txt` to match a result you have not recomputed.

### 14–20 December — pick impact and the benchmark

Pick impact and the portfolio benchmark fall out of that same replay.

1. For each symbol, ending wealth is the shares still held at the as-of close, plus the cash from that symbol's dividends. Subtract the fund path of the dollars that bought that symbol. That difference is the pick impact.
2. The benchmark replays every buy into the comparison fund. Selection is the actual ending value minus that fund ending value.
3. Put symbol-less cash on both ending values, so a deposit, interest, or tax does not become a pick.
4. Add a test that sums the symbol impacts and asserts the sum equals selection.
5. Run the pick-impact fixture and the benchmark fixture. Both must match to the cent.
6. Commit only when both are green.

Done when the two fixtures match, and the sum test fails if you drop one symbol's impact.

Stop. Do not add a control that swaps an arbitrary pair of symbols. That is the stretch.

### 21–27 December — holiday

A short week. Timing only if last week's fixtures are already green.

1. If the buys-only, pick-impact, or benchmark fixture is red, spend the week on that failure. Do not open timing.
2. If they are green, write timing beside the replay. Contributions are the deposits when the file has any. Otherwise each buy is a contribution on its own date.
3. Both sides hold only the comparison fund. Neither side sells because a stock was sold. One side buys on the contribution dates and holds. The other puts the same dollars in on the first contribution date.
4. The report text says that later cash is treated as if it had already existed on the first date.
5. Assert that the timing gap is stored in its own field and is not added to selection.
6. Run the timing fixture. Commit when it matches.

Done when timing matches, or you wrote down which replay fixture is still red and fixed that instead.

Stop. Do not add sells, the page, or a new rule.

### 28 December – 3 January — sell, dividend, fee

One sell fixture, one dividend fixture, and one fee fixture.

1. A sale's fraction is shares sold divided by shares held that day. Apply that fraction to every lot, and to the fund shares that came from those lots. Each path uses its own price on the sale date.
2. Selling more shares than are held refuses the run. Wire the fixture that expects that refusal.
3. Run the sell fixture until both paths match `expected.txt`.
4. Add the dividend fixture. The cash lands on the actual holding only. The fund path does not gain a dividend. The pick includes the dividend, and the sum of impacts still equals selection.
5. Add the fee fixture from the tax row. Cash falls on both paths. Selection does not change.
6. If the sell fixture is still red on 3 January, leave the expected file alone and write the failure at the top of the spec. The buys-only demo is the fallback.

Done when dividend and fee match. The sell fixture either matches or is an honest, written failure.

Stop. Do not make the sale consume one chosen lot. Both engines would then be free to pick different lots.

### 4–10 January — Python

Python recomputes every fixture without reading the Java methods.

1. Read `spec/ANALYTICS.md` and the fixture files. Do not read the Java replay methods while you write the Python.
2. Put the verifier in `python/verifier/`. It reads the same `activities.csv`, `prices.csv`, and `expected.txt`.
3. Write `scripts/compare` so it runs Java and Python and, on a mismatch, prints the fixture name and the field name.
4. Run it on every fixture. A mismatch is a bug in one engine or in the spec. Fix the wrong one. Do not special-case a fixture.
5. Copy one fixture, change one expected cent, and confirm compare fails on that field. Delete the copy.
6. Commit when compare is green on the real fixtures.

Done when compare passes, and you have seen it fail on a field you changed on purpose.

Stop. Do not import Java from Python, and do not start the page.

### 11–17 January — page and label

One React and TypeScript page that shows Java's report, plus the pull-request label.

1. Create `web/` with React and TypeScript. Next.js is fine. The page has no formula in it.
2. One page, no Next button. The transaction table lists every activity from the report: date, type, symbol, shares, trade price, and cash.
3. The three answer blocks stay empty until a comparison fund is chosen. Then they show pick impact, the benchmark pair, and timing.
4. Opening a symbol lists the lots under its impact.
5. The page runs the Java program and reads its report. It does not recompute.
6. Add a GitHub Actions workflow. If the pull request touches `java/src/main/java/mark/`, the price reader, rounding, or the report fields, apply the label `money-path`. A change that is only markdown does not get the label.
7. Change one number in a fixture, rerun Java, and confirm the page changed only because the report changed.

Done when a classmate can upload the demo file, pick a fund, and read the three answers. The label appears on a math change and not on a wording change.

Stop. Do not put a second copy of the arithmetic in the page. Do not build the replacement control or an assistant.

### 18–24 January — cold run

A cold run of the demo, a README, and a 90-second backup.

1. From a fresh terminal, run compare, then open the page, and do the eight-minute path with a timer.
2. Fix only a break in that path. A new idea goes on a list, not into the code.
3. Write the README in the words you will say. How to run Java. How to run compare. What the three answers are. The value will not match the live Wealthsimple app. Prices were copied by hand. There is no assistant and no replacement control.
4. Record about 90 seconds of the same clicks, in case the live file will not load.
5. Say the four tradeoffs out loud once: nearest cent and say so, the sell rule written before the code, timing kept separate, Java serves the page.

Done when you can do the click path without this file open, and the recording exists.

Stop. Do not restyle the page, and do not add a fixture, unless the cold run actually failed.

### 25–31 January — freeze

Freeze. Rehearse twice.

1. Rehearse the 30 minutes on two different days.
2. If something breaks, use the rollback table. Return to the last green commit rather than patching on the day.
3. Read the README and the spec once. They should still describe the program you will open.
4. Leave replacement replay and the assistant unbuilt.

Done when two full rehearsals are done, and no rule changed during the week.

Stop. No new activity type, no new fund, no live price call.

### February and March — reviews

The reviews. The program stays as it was at the freeze.

1. The week of a review, rehearse once and change nothing.
2. If a reviewer asks about a sale and that fixture is red, show the spec and say so.
3. If they ask about the Wealthsimple balance, say the page uses the price file on the last date in the activities file, and it will not match the live app.
4. After a review, a fix is allowed only for something that failed in the room. It is not a new feature.

Done when the demo you give is the demo you froze.

Stop. Do not start the assistant or the replacement control in the week of a review.

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

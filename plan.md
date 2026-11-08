# Portfolio Mark

This file is the source of truth for scope, architecture, workflow, and the schedule. Read it before changing the product.

`spec/ANALYTICS.md` is not the contract yet. The 6 November write-up described three separate answers and a fund path with no distributions. That text is withdrawn. Rewrite the spec from this file in the week of 9 November 2026. Until the morning recomputation works, the Domain rules, Worked example, and Fixtures sections below are the draft contract.

When the morning recomputation works, the spec becomes the contract for formulas, refusals, fixtures, and expected cents. In that same commit, replace those three sections of this file with a one-line pointer to the spec, so every rule lives in one place. From then on:

- Java and Python are wrong when they disagree with the spec.
- If the spec takes on scope this file does not allow, this file wins and the spec is cut back.
- A scope or workflow change updates this file. A formula, refusal, fixture, or expected-cent change updates the spec. A change that touches both updates both in the same commit, before the code.

The early Python sketch and `demo.html` have been removed. Do not restore them. Do not treat any fund, including QQC, as a built-in default. Until the week of 18 January, `README.md` only points here.

Ready by 31 January 2027. Interviews are most likely February and March 2027. One person, with agents writing most of the code under Who writes what. About 101 project hours over twelve weeks, plus about 3 hours a week of OOP practice, with one empty buffer week. The work runs the way a team with agents works: issues, pull requests, reviews, and releases.

## What it is

Portfolio Mark answers one question about a real brokerage history: did your choices beat simply buying a fund you pick, on your own dates, and which choices made the difference?

A person uploads one Wealthsimple activities CSV, picks one comparison fund, and picks an as-of date. Java replays the history four ways and produces every number. The page shows one bridge. Each step is the gap between two neighbouring paths, so the steps add up.

The four paths, all valued on the as-of date:

1. **All on day one.** Every contribution dollar buys the fund on the first contribution date and is held.
2. **Fund on your dates.** Each contribution buys the fund on its own date and is held.
3. **Benchmark.** Each purchase's dollars buy the fund that day. Each sale sells the same fraction of the matching fund shares. Proceeds are cash.
4. **Your account.** What happened: shares from the CSV valued at the as-of close, plus all cash.

The three steps:

1. **When the money arrived.** Fund on your dates minus all on day one.
2. **Cash and sales.** Benchmark minus fund on your dates.
3. **Stock selection.** Your account minus the benchmark. This equals the sum of the pick impacts.

The **headline** is cash and sales plus stock selection: your choices versus the fund on your dates. A **pick impact** is one holding's real path minus the same dollars in the fund.

There is one page and no Next button. The page does not calculate. Java writes a JSON report. The page shows it.

## What this version is not

- Not a second Wealthsimple download. The holdings report is not an input.
- Not a live balance. The number will not match the Wealthsimple app. The page says so.
- Not a live price feed. Closes and distributions are downloaded once and saved with the source and the date. The program never calls the network. Only `scripts/fetch-prices` does, when you run it.
- Not a partial history. When the file has deposits and your cash goes below zero, earlier history is missing, and the run refuses.
- Not Northrail. A Northrail entry is an account, cents, a transfer, and a time. It has no symbol, shares, or price. Do not import its schema, Kotlin services, or ops screens. Integer cents, history that is never edited, and a label on money changes are the parts that carry over.
- Not replacement replay. Swapping one named stock for another named stock is stretch, after the bridge is green.
- Not an assistant. An assistant may later explain a report the two engines already matched. It does not compute. It is not on the January page.
- Not withdrawals. An EFT with negative cash refuses the run.
- Not a database, not Spring, not CodeQL.
- Not deployed. The page runs on your machine, because it accepts brokerage files.

## Stack

| Layer | Choice |
| --- | --- |
| Engine | Java 21, Gradle, JUnit 5. Package `mark`. Produces every number. The page runs the jar. Path math uses an exact rational type, never `BigDecimal` or `double`. |
| Check | Python 3 in `python/verifier/`, with `fractions`, pytest, and Hypothesis. Same fixtures, then generated ledgers. Never serves the page. Never imports Java. |
| Page | React and TypeScript. Next.js is allowed. No formula in the page. `web/src/money.ts` is the only file that formats cents. |
| History | The normalized activity list produced from the CSV. |
| Prices | `prices.csv` and `distributions.csv`, raw closes, downloaded once. The demo's three comparison funds, none with a split inside the history, are committed in `demo/`. The real export's price files stay in `exports/`. |
| CI | GitHub Actions. Every push runs the format and lint checks, the tests, compare, and 200 generated ledgers. `actions/labeler` applies `money-path` from the paths in Checks. A pull request that touches those paths also runs a few thousand generated ledgers. `main` is protected: changes land only through pull requests, and from the week of 23 November only with CI green. `./gradlew test` runs from that week; the rest arrives in the week of 4 January. |
| Format | Spotless with google-java-format for Java. Ruff for Python. ESLint and Prettier for the page. CI fails on unformatted code. |

## Architecture

Java produces every number. Python is a second engine written from the spec. There is no database. The page calls the Java program and displays its JSON report. It does no arithmetic.

A Next.js route runs the jar with `execFile` and reads the JSON from stdout. That is what "Java serves the page" means. It is not an HTTP server.

A **case** is a folder: `activities.csv`, `prices.csv`, `distributions.csv` when a fund pays one, and `run.json`, which holds the comparison fund and the as-of date, for example `{"asOf": "2021-12-01", "fund": "FUND"}`. A **fixture** is a case plus `expected.json`. Java, Python, compare, the generator, and the page all use this one format. Without `run.json`, a case only reads: the report holds `activities` and `warnings`, or a reader refusal. The page uses that to show the transaction table before a fund is chosen.

```text
                         case folder
   activities.csv, prices.csv, distributions.csv, run.json
              |                               |
              v                               v
         Java engine                    Python engine
              |                               |
         JSON report                     JSON report
           |     |                            |
           v     +---------> compare <--------+
          page       (fixtures, generated ledgers, the real export)
```

Compare is not on the path to the page.

The as-of date is never earlier than the last activity. The fund is a symbol the person picks. A fixture's `run.json` sets one. There is no product default.

| Path | What it holds |
| --- | --- |
| `spec/ANALYTICS.md` | The contract, once rewritten: inputs, rules, paths, refusals, fixtures, and the measured cash tolerance |
| `spec/fixtures/<case>/` | One case plus `expected.json` |
| `spec/fixtures/pending.txt` | One line per fixture and field the runner skips because its part is not built yet. Only you edit it. Empty from 20 December |
| `java/src/main/java/mark/ledger/` | `Activity`, a sealed interface with one record per type, and the CSV reader |
| `java/src/main/java/mark/market/` | Closes and distributions, with the lookup rules |
| `java/src/main/java/mark/analytics/` | The four paths, the steps, the picks, and the report |
| `java/src/main/java/mark/Main.java` | One case folder in, its report as JSON on stdout. Given a folder of case folders, one report per line |
| `java/src/test/java/mark/` | One test that runs every fixture folder |
| `python/verifier/` | The second engine, written from the spec, with no Java imported |
| `python/tests/` | pytest over the fixtures, and the generated-ledger harness |
| `scripts/compare` | On a fixture, checks both engines against `expected.json` and against each other. On any other case, checks the two engines against each other. Prints the case and the first field that differs |
| `scripts/fetch-prices` | Downloads closes and distributions once, when you run it. The program and CI never run it |
| `demo/` | The synthetic demo history as a case, with its price and distribution files and their sources |
| `web/` | The page. `web/src/money.ts` formats cents. |
| `docs/decisions/` | One decision record per choice a reviewer could question: the context, the decision, the alternatives, and the cost |
| `docs/mistakes.md` | One short postmortem per surprise: what happened, how it was caught, the root cause, and what now prevents it |
| `docs/questions.md` | Every gate question you missed, and the questions a reviewer is likely to ask, answered in your words. The question bank for the review |
| `.github/` | The CI workflow, the labeler's money-path globs, and the issue and pull request templates |
| `exports/` | The real CSV and its own price files. Git ignores this directory and `.cursorignore` lists it. Never commit it. |

The report fields are `asOf`, `fund`, `paths` (`allOnDayOne`, `fundOnYourDates`, `benchmark`, `account`), `steps` (`whenMoneyArrived`, `cashAndSales`, `stockSelection`), `headline`, `picks`, `bestPick`, `largestDrag`, `activities`, `warnings`, and `error`. `picks` lists, per symbol, its impact, each lot's value on both paths, and its dividends. Every money field is whole cents. `error` is present instead of the rest when the run refuses. A refusal fixture's `expected.json` holds that one error. Every other fixture's `expected.json` holds the fields its case checks, in the report's shape. The spec defines the full shape. Errors and warnings are compared by code and fields, never by wording.

## Domain rules

This section is the draft contract until the spec takes over. Then it moves to the spec, and only a pointer stays here.

### Inputs

`activities.csv` uses the Wealthsimple header: `effective_date,effective_time,settlement_date,account_id,account_type,activity_type,activity_sub_type,description,direction,symbol,name,currency,quantity,unit_price,commission,net_cash_amount`.

`prices.csv` is `symbol,date,close`, with raw closes. Never use adjusted closes: an adjusted close already folds in distributions, and the fund paths add distributions separately. `distributions.csv` is `symbol,ex_date,amount_per_share`, for the comparison funds. `run.json` holds `asOf` and `fund`.

One account and one currency per run. Canadian dollars in the demo. The account id in committed fixtures is `DEMO`.

### Reading the file

The normalized row is date, time, type, symbol, currency, quantity, unit price, cash, and the source row number. Cash is `net_cash_amount` in signed whole cents. A buy is negative. Symbol is empty on a pure cash row. The source row is the CSV line number.

`Trade` plus `BUY` is a buy. `Trade` plus `SELL` is a sale. `MoneyMovement` plus `EFT` with positive cash is a deposit. Negative cash is a withdrawal, and this version refuses it. `Dividend`, `Interest`, and `Tax` map to those types. On those rows the quantity column copies the cash and is ignored. Any other type, or any sub-type the spec does not list, refuses the run, and the error names the type and the sub-type. The week-1 inventory of the real export decides which sub-types the spec lists.

Every number is parsed from its text into an exact value. `185.8904` never passes through a float or a double.

The trade date is `effective_date`. Ignore `settlement_date`. Ignore an executed-at phrase in the description.

Sort by `effective_date`, then `effective_time`. A row with no time sorts before the timed rows on its date. Rows with the same timestamp keep file order. Lots are not merged.

Cash is always `net_cash_amount`. It is what moved, so it is never recomputed. A buy's cash should equal shares times price plus commission, and a sale's cash should equal shares times price minus commission. Quantities and prices both print to four decimals, so the starting tolerance is `0.00005 × price` plus `0.00005 × shares` plus half a cent. In week 1, measure the worst gap on the real export and write that tolerance into the spec. Outside the tolerance, the run refuses.

When the file has deposits, your cash, the running sum of `net_cash_amount`, must be zero or more at the end of every day. Below zero means the file is missing an earlier deposit or transfer, and the run refuses.

### Money

Shares and prices stay exactly as printed. `3.9152` stays `3.9152`. Path values stay exact fractions and are not rounded along the way. In Java that is a small rational type over `BigInteger`. A `BigDecimal` with a fixed precision can land a hair off an exact half cent and round the wrong way, which Python's `Fraction` never does.

A lot's value on a path is its shares still held, at the as-of close, plus its share of sale proceeds, plus, on a fund path, the distributions it was paid. On your path, a sale's cash is split across that symbol's lots in proportion to the shares each lot sold. A lot's value stays exact until it is rounded once, to whole cents, half to even. `12.5¢` becomes `12¢`. `13.5¢` becomes `14¢`.

Every total, step, and impact is a sum of those cents plus cash that was already whole cents. The impacts sum to stock selection exactly, and the bridge adds up exactly. The account total can sit up to half a cent per lot away from the exact value. That is accepted.

### Dates and prices

A fund purchase or sale uses the fund's close that day, or the next close within 7 days. A deposit on Sunday 14 June 2026 buys at the Monday 15 June close. If that next close falls after the as-of date, the trade uses the valuation close instead, so the money neither gains nor loses.

Valuation uses the close on the as-of date, or the last close in the 7 days before it. The as-of date is an input.

A close the replay needs that is not within those 7 days counts as missing. A missing close refuses the run and names the symbol and the date it was needed for. No close is invented, and no close further away is used.

Each downloaded price file is saved once, with a `SOURCES.md` beside it that says where it came from and when.

A close more than 20% away from the same symbol's previous close prints a warning, when that previous close is at most 7 days earlier. Hand-made fixture prices are often months apart, and the 7-day limit keeps them quiet. A warning does not stop the run. Splits are not supported. A split row is an unknown type and refuses.

### Paths

Contributions are the deposits when the file has any. Otherwise each purchase is a contribution on its own date.

Each contribution is its own fund lot on both contribution paths. All on day one buys every one of those lots on the first contribution date. It treats later cash as if it had already existed then. Say that out loud.

Neither contribution path sells because a stock was sold.

Each purchase is its own lot on your path and its own fund lot on the benchmark.

Every path starts with no cash, and a contribution adds its cash on its date. With no deposits, each purchase is its own contribution, so it brings exactly the cash it spends, even when an earlier sale's proceeds could have paid for it. A path's value is its lots' cents plus its cash outside any lot: the contributions, minus what its purchases spent, plus interest and tax, plus, on your account, the dividends. Sale proceeds stay inside the lot that sold, so they are never counted twice. That is why the cash outside any lot can go below zero when proceeds pay for a later purchase, while your cash in the refusal rule above cannot.

A sale's fraction is the shares sold divided by the shares held just before the sale, in sort order. Every lot of that symbol sells that fraction, and so does every fund lot that came from it, each at its own price. Proceeds are cash and earn nothing. Selling more shares than are held refuses the run. A sale never consumes one chosen lot.

A pick impact is one symbol's real path minus its fund path. The real path is that symbol's lots and dividends. The fund path is the same dollars on the same dates, with the same sale fractions, plus the distributions those fund lots were paid.

A symbol's dividends from the CSV are cash on its real path and one trace line under that symbol, outside any lot.

Every fund path is paid the fund's distributions, and neither side reinvests. For each ex-date on or before the as-of date, a fund lot gets its shares held at the end of the day before the ex-date, times the amount, as cash, inside that lot's single rounding.

Interest and tax land on every path, whether or not the row names a symbol, so they never move a step.

Deposited cash that is never spent stays cash on your account and the benchmark. The contribution paths invest all of it, so it shows up in cash and sales.

When sale proceeds pay for a later purchase, the benchmark's cash can go below zero. That is allowed. The benchmark is a measurement, not a portfolio you could have held.

Holding the comparison fund itself has an impact of zero, apart from the gap between the fill and that day's close and the cent rounding of the trade's cash.

`bestPick` is the most positive impact. `largestDrag` is the most negative. The report says none when no impact has that sign. Ties go to the symbol that sorts first in plain ASCII order.

### Refusals

A refusal stops the whole run and returns one error: a code, the fields its row names, and a message for people. There is no partial report. Each code is its fixture's name without `refuse-`, for example `missing-close`.

| Refusal | The error names |
| --- | --- |
| An unknown or missing column | The column |
| A second currency | The currency and the row |
| A second account | The row |
| An unknown type or sub-type | The type, the sub-type, and the row |
| A withdrawal | The row |
| Cash outside the tolerance | The row and the size of the gap |
| Cash below zero, when the file has deposits | The date and the shortfall |
| An as-of date before the last activity | Both dates |
| A sale larger than the shares held | The symbol and the date |
| A missing close | The symbol and the date it was needed for |

## Worked example

Made-up history. No deposits, so each purchase is a contribution on its date. No dividend and no distribution. Trade prices come from the CSV. Closes come from the price file. In the fixture, use 2021, when all four dates were weekdays. Recompute this from the spec alone once the spec exists.

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

Self-holding check. $1,000 of the comparison fund, filled at the $50 close, is 20 shares. The fund pays $0.50 a share and then closes at $55. Your path is 20 × $55 plus the $10 distribution, which is $1,110. The fund path is the same $1,110. The impact is zero.

## Fixtures

One folder per case. Compute every expected number on paper before typing it. Twenty-three folders: thirteen cases and ten refusals.

| Fixture | What it checks |
| --- | --- |
| `walkthrough` | The worked example: four paths, three steps, the headline, and both picks |
| `buys-only` | The walkthrough without the 1 June sale. Cash and sales is zero |
| `half-cent` | Buys whose lot values land exactly on half a cent, on your path and on the benchmark. 500.5¢ rounds to 500¢ and 501.5¢ to 502¢. Half up would make the first 501¢ |
| `deposit-quantity` | A `MoneyMovement` row with quantity 700 and cash 700. 70000 cents of cash and zero shares |
| `dividend-quantity` | A dividend with quantity 30.90 and cash 30.90. Cash on that symbol, not 30.90 shares |
| `interest-and-tax` | 1 cent of interest and a 2-cent non-resident tax, no symbol. Every path moves by the same cents, and no step changes |
| `same-second` | Four buys with the same timestamp, and an interest row with no time on the same date. The interest sorts first, then four lots, in file order |
| `weekend-deposit` | $19,260 deposited on Sunday 14 June 2026, spent on Monday 22 June, one cent left. Fund on your dates buys at the 15 June close. The benchmark buys at the 22 June close. The cent stays cash |
| `dividend-then-buy` | A $30.90 dividend, then a $30.89 buy the next day. The dividend sits inside that symbol's pick. The buy is its own lot |
| `dividend-and-distribution` | A stock dividend and a fund distribution in the same window. Each lands only on its own path |
| `self-holding` | A buy of the comparison fund itself at that day's close, for a share count whose cost is whole cents, with one distribution. Impact exactly zero |
| `as-of-input` | An as-of date on a Sunday, the day after the last row. Valuation uses the last close before it. The last row is a Saturday deposit: its next close falls after the as-of date, so fund on your dates buys that lot at the valuation close. One fund close sits 25% above its previous close, two days earlier, and the report warns |
| `cash-check` | 2.6897 shares at 185.8904 with cash −499.99. Accepted |
| `refuse-column` | A header with one column the spec does not list. The error names the column |
| `refuse-currency` | One USD row. The error names the currency and the row |
| `refuse-account` | A second account id. The error names the row |
| `refuse-type` | An activity type or sub-type the spec does not list. The error names both |
| `refuse-withdrawal` | An EFT with negative cash. The error names the row |
| `refuse-cash` | The `cash-check` row with its cash outside the tolerance. The error names the row and the gap |
| `refuse-cash-below-zero` | A deposit, then a buy for more than the cash on hand. The error names the date and the shortfall |
| `refuse-as-of` | An as-of date before the last activity. The error names both dates |
| `refuse-oversell` | A sale larger than the shares held. The error names the symbol and the date |
| `refuse-missing-close` | The fund's first close after a purchase is 8 days later. The error names the symbol and the date it was needed for |

## Checks

Five layers. Hand fixtures catch wrong arithmetic on cases you thought of. The self-holding and half-cent fixtures catch a rule both engines get wrong the same way. Invariants catch whole classes of bugs on any input. Generated ledgers catch disagreements on inputs nobody wrote by hand. CI keeps a red `main` from moving forward.

Both engines' reports are checked against these on every generated ledger. The sentences below are yours. Add a new one here before any code. The agent turns each sentence into code, and you confirm the code says what the sentence says.

- The pick impacts sum to stock selection, exactly.
- All on day one plus the three steps equals your account, exactly.
- A stock that trades, closes, and pays exactly like the fund has zero impact, exactly, when the generator uses whole shares so the cash is exact.
- With a fund whose close never changes and that pays nothing, "when the money arrived" and "cash and sales" are both zero, exactly.
- Splitting a buy into two same-day buys at the same price moves no path value by more than a cent per extra lot.
- Reordering buys that share a timestamp changes no path, step, or impact.
- A ledger with a single contribution has "when the money arrived" equal to zero.
- Adding an interest or tax row moves all four paths by the same cents, and no step.

Hypothesis draws a ledger: one to five symbols, sometimes including the comparison fund itself; deposits, buys, sales no larger than the shares held, dividends, and interest; and a fund with distributions, with a close on every date the ledger needs. When the ledger has deposits, purchases never take cash below zero. Some share counts and prices are chosen so that a lot's value lands exactly on a half cent, because random values almost never do. The harness writes each ledger as a case, runs both engines, compares every field, and checks every invariant. On a failure, Hypothesis shrinks the ledger. Save that small ledger as a fixture, compute its answer on paper, fix the engine that is wrong, and add an entry to `docs/mistakes.md`.

The real export runs through both engines before each rehearsal. It has no expected file, so agreement is the check. It is never committed and never runs in CI.

`money-path` applies when the diff touches `spec/**`, `java/src/main/**`, `java/src/test/**`, `python/verifier/**`, `python/tests/**`, `scripts/compare`, `demo/**`, or `web/src/money.ts`. Editing an expected file rewrites the answer key, and weakening a test, the runner, or compare does the same thing quietly. The README, `docs/`, and the rest of `web/` are not money-path.

The long generated run triggers from a `paths` filter on the same globs, not from the label: a label added during a run is not part of that run's event. Protecting `main` on a private repository needs GitHub Pro, which the GitHub Student Developer Pack includes. A public repository can protect `main` on the free plan.

## Workflows

### Changing a rule

1. Change this file if the scope or the workflow changed.
2. Change `spec/ANALYTICS.md` if a formula, a refusal, a fixture, or an expected cent changed.
3. Recompute the affected `expected.json` by hand.
4. Change Java. Then change Python from the spec change, not from the Java diff. The Python chat gets the spec diff, never the Java diff.
5. The fixture tests, and `scripts/compare` once it exists, must be green before the change is done.
6. Do not edit an expected cent to match a result you have not recomputed on paper.

### Issues and pull requests

Every change moves the way it does on a team that works with agents: an issue, a branch, a pull request, a review, and a merge.

1. Every piece of work starts as a GitHub issue, written so an agent could build it from the issue alone: the context, what to build, the signatures or files, the fixtures that prove it under Done when, and what is out of scope. You write the money-path issues. An agent may draft the others, and you edit them before work starts.
2. One issue, one branch, one pull request. Name the branch after the issue, for example `14-sale-fraction`. Keep a pull request small enough to read in one sitting.
3. Commit on the branch whenever the tests you touched are green. Nothing is pushed to `main` directly.
4. The pull request follows the template in `.github/`: What changed, Why, How I tested, Risk, and Written by. An agent may draft What changed. Why, How I tested, and Risk are yours, even when an agent wrote the code. Written by lists the files you typed and the files an agent typed. Write `Closes #14`, so the merge closes the issue.
5. Merge when CI is green, the review is done, and the issue's Done when is true. Squash-merge, so `main` holds one commit per issue.

### Reviews

Nothing merges without a review from someone who did not write it.

1. On every pull request, start a new agent chat that has not seen the work. Ask it to review the diff against `spec/ANALYTICS.md` and the issue: correctness gaps, spec mismatches, and missing tests, not style. On a pull request that touches an `expected.json`, it checks the input files and the report's shape, never the cents. You decide what changes. In your files, you make the change yourself.
2. You review every pull request an agent wrote. On a money-path pull request, read every line and be able to explain any of them. On any other, read the whole diff and be able to say what each file does.
3. Ask one person, a coworker, a classmate, or a mentor, to review three pull requests: the spec, the buys-only replay, and the sale fraction. Answer every comment in the thread, and either change the code or say why not. What they catch goes in `docs/mistakes.md`. If nobody can, the agent reviews stand, and you do not claim a human review.

### Decisions, status, and releases

- **Decisions.** In the week you make a choice a reviewer could question, write a decision record in `docs/decisions/`, about half a page: the context, the decision, the alternatives and why they lost, and what it costs. The first five are the broker's cash is the truth, the same fraction of every lot, and one bridge, all in the spec week; exact fractions instead of `BigDecimal`, in the replay week; and comparing the engines on generated ledgers, in the CI week. The README's tradeoffs link to them.
- **Status.** Every Sunday, comment four lines on a pinned issue called Status: what shipped, what is next, the biggest risk, and hours planned against hours spent. About ten minutes. It is the weekly update you would post for a team.
- **Releases.** When a week's done line is true, tag `main` and let GitHub generate the release notes from the merged pull requests.

| Done line | Tag |
| --- | --- |
| Buys-only replay, 6 December | `v0.1.0` |
| Bridge and sales, 13 December | `v0.2.0` |
| All 23 fixtures and the bare page, 20 December | `v0.3.0` |
| Python, 3 January | `v0.4.0` |
| Generated ledgers and CI, 10 January | `v0.5.0` |
| The page, 17 January | `v0.6.0` |
| Freeze, 31 January | `v1.0.0` |

The reviews demo `v1.0.0`.

### Engines

Java is the product. Python is a second implementation of the spec. An agent writes it in its own chat, on a different model from the one that wrote any Java, with only this file, the spec, and the fixtures in front of it. Two agents on the same model reading the same spec tend to make the same mistake, and then compare agrees on the wrong answer. Your hand-computed fixtures are the one check no agent touched. Keep two planted-bug checks. Change one expected cent and confirm compare fails. Switch Python's rounding to half up and confirm the `half-cent` fixture and the generated ledgers catch it. Restore both.

Commit when the fixture you touched is green. The message names the fixture.

### Page

The page runs Java and displays the report. The transaction table comes first, from a read-only run without `run.json`. The headline, the bridge, and the picks stay empty until a comparison fund and an as-of date are chosen. Opening a symbol lists its lots on both paths, plus its dividends line. Those lines sum to its impact. A saved report is only a fallback when Java cannot run, and the README says that.

### Rollback

| If this fails | Fall back to |
| --- | --- |
| A week goes red | The last commit where CI passed. With `main` protected, that is `main` itself. Do not patch forward on a red main. |
| Something breaks after the freeze | Check out `v1.0.0`. |
| The real CSV reader slips | Demo the synthetic history. Show the real file only as far as it parses. |
| Sales are still red | Demo buys-only. The sale rule stays in the spec. Say that fixture is open. |
| Python disagrees | Do not call the page verified. Show the shrunk case and the field. |
| Java cannot run under the page | The page reads the last report saved from a green run. |
| A price is missing | Show the refusal with the symbol and the date. Do not fill in a close. |
| The bridge is still red in January | Show the spec, the fixtures, and the one fund replay that matches. |

If a week runs short, finish the done line and skip polish. Cut in this order, and do not cut the four things that stay: the spec and its fixtures; Java's four paths, the picks, and the bridge; Python agreeing on the fixtures and on generated ledgers; a page with the headline, the bridge, and one symbol's trace.

1. Replacement replay and the assistant.
2. Page polish.
3. The fund and as-of pickers. The demo's `run.json` can set both.
4. The long generated run. Keep the 200 ledgers on every push.

## Who writes what

You run this project. Agents type most of it. You keep what a reviewer will ask about, plus the code that doubles as practice for the technical round: the rules, the answers, the activity model, the exact arithmetic, the core replay, and the diagnosis of every failure. Reading code teaches less than writing it, and debugging suffers most. The gate and the diagnose-first rule are there to close that gap.

The product rule still holds for you: an agent may explain and propose, and you are the checker. It never computes an expected cent.

You write these by hand:

- `spec/ANALYTICS.md`, in your own words.
- Every `expected.json`, computed on paper, and `spec/fixtures/pending.txt`.
- The `Activity` records and their first test.
- `Rational` and its one rounding method.
- The buys-only replay: your account, the benchmark, and the pick impacts.
- The sale fraction.
- The invariant sentences in Checks.
- `docs/mistakes.md`, `docs/questions.md`, the decision records, the README's tradeoffs, and the weekly status.
- The money-path issues, and the Why, How I tested, and Risk of every pull request.

An agent writes the rest, and you review all of it: the Gradle project and the fixture runner, fixture input files typed from your description, the reader, the market lookups, the contribution paths, the steps and the headline, dividends and distributions, `Main.java`, the Python verifier, `scripts/compare`, `scripts/fetch-prices`, the generator and the invariant code, CI, and the page.

For your code: sketch, test, struggle, ask.

1. Sketch the classes on paper for 10 minutes, as you would in the technical round.
2. Write the test first. For the replay, that is the fixture's `expected.json`.
3. Struggle for 30 to 45 minutes with no assistant.
4. Then ask one narrow question. The agent explains. You still write the code.
5. Type all of it, with Tab completion off. No agent edits in these files.
6. Explain it back. You write the pull request's Why and How I tested.

For an agent's code:

1. In `java/src/main/**`, you write the signatures and name the fixtures that prove them. The agent fills in the bodies. To extend your replay, it adds code behind signatures you wrote and never rewrites yours. Plumbing (Gradle, workflow YAML, the page) needs no signatures.
2. One issue per pull request. Review it as Reviews says before you merge it. Its Why, How I tested, and Risk are yours.
3. When money code fails, you diagnose first. Write down which rule broke and where before the agent proposes a fix, and if no failing test or fixture shows the bug yet, add one. If you are stuck after 20 minutes, ask for a hint, not a fix. You fix your files, and the agent fixes its own. Your diagnosis goes in the pull request's Why.
4. The Python verifier comes from its own chat, on a different model from the one that wrote any Java, given only this file, the spec, and the fixtures.

| When | Idea | Then |
| --- | --- | --- |
| 9 Nov | What a replay is, from the walkthrough | Write the walkthrough into the spec in your own sentences |
| 23 Nov | Why a sealed interface | Write the `Activity` records and their test before the agent writes the reader |
| 30 Nov | Why exact fractions, and why round only once | Write `Rational` and the buys-only replay, then delete the replay and rewrite it from memory in under an hour |
| 7 Dec | The same fraction of every lot | The walkthrough's sale on paper, then the sale fraction by hand |
| 28 Dec | Why the second engine must not read the Java | A new chat, on a different model, gets only the spec and the fixtures |
| 4 Jan | What an invariant is, and how shrinking works | Diagnose every shrunk failure before anything is fixed |
| 4 Jan | Which files are money | You choose the money-path globs. The agent writes the workflow YAML |

Keep `docs/mistakes.md`, written like a short postmortem: what happened, how it was caught, the root cause, and what now stops it from happening again. Write it without blame: the question is what let it through, not who. Every failure the generator shrinks gets an entry, and so does every bug that you or a review catch in an agent's code. These are your answers when a reviewer asks what broke.

### The weekly gate

Every build week ends with the gate, and the next week starts only after it passes. Ask the agent to run it. About an hour.

1. **Explain.** Without notes, three minutes on this week's part: what goes in, what comes out, and why it is built that way.
2. **Predict.** The agent writes a ledger of three to five rows you have not seen. Compute the affected numbers on paper, then run the engine. If they disagree, find out which of you is wrong.
3. **Trace.** From memory, name the classes and methods one of those rows passes through. Then check the code.
4. **Find the bug.** On a scratch branch, the agent plants one bug in this week's code. Find it within 20 minutes using the tests and your own reading, with no agent help. Delete the branch.
5. **Change it by hand.** On a scratch branch, make one small change the agent names, with no agent help, and get the tests green. Delete the branch.
6. **Say why not.** Name the main alternative to this week's biggest decision, and why it lost.

Pass means all six, without notes or agent help. A miss means the next session starts with that item again. Write every question you missed into `docs/questions.md`, with the answer in your words.

The sentence you can say, if it is true: "I wrote the spec, every expected cent, the activity model, the exact arithmetic, the buys-only replay, and the sale rule. Agents wrote the rest of the Java against signatures and fixtures I gave them, and I read every line. A different model wrote the Python check from the spec alone. Every fixture has to match answers I computed by hand, and the two engines have to agree on thousands of generated ledgers. Every change went through a reviewed pull request, and no code merged with CI red."

## OOP practice

This is not project work, and practice code stays out of this repository. It is here because it shares the weeks, and because the technical round comes first: the project review only happens if you pass it.

What candidates reported in 2025 and 2026. Confirm it with your recruiter.

- One hour with one interviewer, in your language of choice. Yours is Java.
- A shared editor, CoderPad in two reports. Two autumn 2026 reports say an AI agent was allowed in the round.
- Object-oriented. You explain why you chose each design.
- One problem that grows: a new requirement arrives partway through.
- A 2025 intern question: given a list of stock transactions, return the cash left and the shares of each stock. Then the transactions arrive one at a time. Then a question about rounding.

Ask the recruiter four things: whether an agent is allowed, and which one; whether you can run code and tests; whether the problem grows in levels; and how much of the review's 30 minutes is your presentation.

1. In the week of 9 November, do the transaction problem cold, in Java, with a 60-minute timer. That is your baseline.
2. Then two timed problems a week. Spend the first 10 minutes designing the classes yourself, out loud. If the round allows an agent, do one of the two with an agent and one without. With an agent, write the class outline first, ask for one small piece at a time, and read every line before you run it.
3. After each problem, spend 20 minutes on what was slow and what would have extended better. Keep that log outside this repository.
4. If the baseline went well, with three levels done in 45 minutes and a reason for every class, do one problem a week until February.
5. In an exam week, do one problem.
6. In the three weeks before the round, do three mock interviews with a person who adds a requirement at minute 35.

The order: the transaction problem and its follow-ups. A list of bank operations: deposit, withdraw, transfer, and a history, then scheduled payments, then an overdraft rule. A robot on a grid: turn and move commands, then obstacles, then charging. An order book that matches at the best price, then partial fills, then cancels. Then a vending machine, a parking lot, an LRU cache, and a library checkout.

Know these cold in Java: records, enums, sealed interfaces, interfaces versus abstract classes, `equals` and `hashCode`, `Map`, `TreeMap`, `Deque`, exceptions, and `BigDecimal` with a `RoundingMode`. The rounding answer: a `double` cannot hold 0.1 exactly. A `BigDecimal` holds any terminating decimal exactly, but dividing needs a rounding mode, because 1/3 never terminates. A rational type never rounds until you ask. In the round, whole cents in a `long`, or `BigDecimal` with an explicit `RoundingMode`, is the right tool. `Rational` is for this project's exact half cents.

Your week-3 and week-4 code is the transaction problem with exact money. Treat it as practice.

About 3 hours a week, more in the three weeks before the round.

## Weeks

Do the week from the top. Each week starts by opening its issues. The week is done only when its done line is true. Build weeks end with the gate. The hours are yours. They include the gate and about 45 minutes of issues, pull requests, reviews, and the status note, and they leave out OOP practice.

### 9–15 November — the spec

Write `spec/ANALYTICS.md` from the domain rules, in your own words. No application code. About 14 hours, plus the OOP baseline.

1. Create `demo/` and `docs/`. Git ignores empty folders, so start each new folder with a one-line README. `exports/` is already gitignored; add it to `.cursorignore` too. Keep the real CSV there.
2. Decide whether the repository is public. A reviewer can open a public one from your résumé, and the private-data rules already keep your real history out of it. Protect `main`, so changes land only through pull requests. Checks says which GitHub plan that needs. An agent adds the issue and pull request templates to `.github/`, with the parts listed in Issues and pull requests.
3. Open the spec with one paragraph: Portfolio Mark reads a case (an activities file, a price file, a distributions file, and `run.json` with a fund and an as-of date) and answers the bridge and the pick impacts. Replacement replay and an assistant are out of this version.
4. Write the inputs, the case format, the normalized row, and the reading rules.
5. Write the money rules: exact fractions, what a lot's value includes, half to even once per lot per path, and every total a sum of cents.
6. Write the date and price rules: raw closes, the 7-day limit on both lookups, the as-of date as an input, and a missing close.
7. Write the four paths, the three steps, and the headline, with the walkthrough numbers, so a reader can recompute them from the spec alone.
8. Write the sale rule and the small sale.
9. Write the pick impact, dividend, distribution, symbol-less cash, and self-holding rules, then the refusal table, with a code for each refusal.
10. Write the report's shape: every field, its type, and one example. Money is integer cents. Quantities and prices are strings, exactly as the CSV printed them, never JSON numbers. Dates are `YYYY-MM-DD`. Activities keep their sort order, picks go by symbol in ASCII order, and lots by purchase. None is `null`. An error is a code, the fields its refusal names, and a message for people. A warning has the same form. An `expected.json` holds only the fields its case checks.
11. Inspect the real export in a spreadsheet or a throwaway script. It is not application code. If an agent writes the script, you run it, and its output stays out of the chat. Confirm its header matches the one in Inputs, column for column. List every activity type and sub-type, so the spec's mapping covers each one. Measure the worst cash-check gap and set the tolerance from it. Check that your cash never goes below zero on any day; if it does, decide in the spec what that means before any code.
12. If the export holds a second account, a second currency, a split, or another row the spec refuses, choose now the account and the dates the real export will use, so it reads without refusing. Save that slice in `exports/`. If no slice works, the real export appears in the demo only as a refusal, and every later step that runs it checks that both engines refuse the same way.
13. Ask an agent to read the spec and list every gap, contradiction, and undefined word. It writes questions, not rules. Answer each one in the spec, in your words.
14. Write the first three decision records: the broker's cash is the truth, the same fraction of every lot, and one bridge.
15. Close the file. The next morning, recompute the four walkthrough paths and the $8,000 sale path using only the spec. If you need this file to do it, the spec is not done.
16. When the recomputation works, the spec is the contract. In that same commit, replace the Domain rules, Worked example, and Fixtures sections of this file with a pointer to the spec.
17. Open the spec's pull request and ask your reviewer to read it like a design document. Answer every comment before you merge.

Done when `spec/ANALYTICS.md` alone lets a new reader recompute the walkthrough and the small sale, it defines the report's shape, it states the measured cash tolerance and every sub-type in your file, this file points to it for every rule, you know which slice of the real export will run, and the spec's pull request is merged.

Stop. No Java class, no React component, and no fixture folder this week.

### 16–22 November — fixtures

One fixture folder per case in the spec, each with an answer you computed on paper. About 13 hours.

1. For each case, write a short description with every number in it: the rows, the closes, the distributions, the fund, and the as-of date.
2. An agent may type each folder from your description: `spec/fixtures/<name>/` with `activities.csv`, `prices.csv`, `distributions.csv` when the case needs one, and `run.json`. Check every file against your description.
3. `activities.csv` uses the Wealthsimple header and account id `DEMO`. Never put a real account id in a fixture.
4. `expected.json` holds only your numbers, in whole cents, in the report's shape. A refusal fixture holds one error instead. Compute from the files, not from your description.
5. Start with `walkthrough`. Recompute every number on paper before you type `expected.json`. Do not copy the numbers out of the spec or a canvas.
6. Then `buys-only` and `half-cent`. Compute them on paper.
7. Then `deposit-quantity`, `dividend-quantity`, `interest-and-tax`, `same-second`, `weekend-deposit`, `dividend-then-buy`, `as-of-input`, and `cash-check`. Use the real export's shape with made-up prices.
8. Then `self-holding` and `dividend-and-distribution`.
9. Then the ten refusals. Each `expected.json` holds the error's code and fields. The wording of its message is not checked.
10. Check the arithmetic again the next day. If the paper total and the file disagree, fix what you typed. If both disagree with the spec, fix the spec first.
11. Each of steps 5 to 9 is its own issue and pull request.

Done when all 23 folders exist, every input file matches its description, and you computed every expected number on paper.

Stop. Do not change an expected number because you are unsure. Uncertainty means that case's rule is unfinished.

### 23–29 November — the reader

Java reads every fixture into activities, closes, and distributions. No replay yet. You write the activity model, and the agent writes the reader. About 7 hours.

1. The agent creates a Java 21 Gradle project in `java/` with JUnit 5, and one test that runs every fixture folder. No Spring and no database. The runner skips only the fixture and field pairs listed in `spec/fixtures/pending.txt`, and prints each one it skips. You write that list, and you remove a line when its part is built. The agent also adds a CI workflow that runs `./gradlew test` on every pull request. From now on, it must pass before a pull request can merge.
2. You write, in `mark.ledger`, a sealed interface `Activity` with one record per type: `Buy`, `Sell`, `Deposit`, `Dividend`, `Interest`, `Tax`. Each carries its source row. A switch over `Activity` must cover every type, so the compiler points at the case you forgot.
3. You write a test that builds three activities by hand and reads them back. No file yet.
4. You write the reader's signature and name the fixtures in step 10. The agent writes the CSV reader. An unknown or missing column refuses and names it. Types and sub-types map as the spec says. A withdrawal, or an unknown type or sub-type, refuses and names it.
5. Every number is parsed from its text, as in `new BigDecimal("185.8904")`, never through a `double`. Cash is `net_cash_amount` in whole cents, the cash check uses the spec's tolerance, and quantity is shares only on trades.
6. Sort by effective date and time. A row with no time goes first on its date. Rows with the same timestamp keep file order.
7. Read `run.json`. Without it, the program only reads, and the report holds activities and warnings.
8. Refuse a second currency, a second account, cash below zero when the file has deposits, and an as-of date before the last activity.
9. In `mark.market`, read `prices.csv` and `distributions.csv`. A fund trade uses that day's close or the next within 7 days. Valuation uses the last close within the 7 days before the as-of date. Anything further refuses as a missing close and names the symbol and the date.
10. Point tests at the `activities` field of `deposit-quantity`, `dividend-quantity`, `cash-check`, and `same-second`, and at the reader refusals: `refuse-column`, `refuse-currency`, `refuse-account`, `refuse-type`, `refuse-withdrawal`, `refuse-cash`, `refuse-cash-below-zero`, and `refuse-as-of`. Test the missing-close lookup directly.
11. Run the gate.

Done when those fields and refusals pass, every activity knows its source row, and the gate passed.

Stop. Nobody writes the replay this week. A reader that invents a share count for a deposit is not done.

### 30 November – 6 December — buys-only replay

The fund replay matches `buys-only` and produces the benchmark and the pick impacts. You write all of it by hand. It is the technical round's transaction problem, with exact money. About 13 hours.

1. In `mark.analytics`, write one function. Inputs: activities, prices, distributions, the fund, and the as-of date. Outputs: your account, the benchmark, and the pick impacts. Buys only for now.
2. Keep every value exact with a small `Rational` record over `BigInteger`. Round only through one method that rounds half to even to whole cents. Do no path math in `BigDecimal`: a fixed precision can land a hair off an exact half cent and round the wrong way.
3. Round each lot's value on each path once, and build every total from those cents.
4. Stock selection is your account minus the benchmark. Add a test that the pick impacts sum to it exactly.
5. Put interest and tax on every path.
6. Run `buys-only`, `half-cent`, and `refuse-missing-close` until they pass. Commit with the fixture names in the message.
7. Delete the replay function and rewrite it from memory in under an hour. Run the same fixtures.
8. Write the decision record for exact fractions instead of `BigDecimal`.
9. Run the gate.

Done when the account, benchmark, stock selection, and picks of `buys-only` and `half-cent` match to the cent, `refuse-missing-close` passes, the sum test fails if you drop one symbol's impact, the rewrite passes the same fixtures, and the gate passed.

Stop. No sales, no contribution paths, and no page.

### 7–13 December — the bridge and sales

The contribution paths, the bridge, and sales by fraction. You write the sale fraction, and the agent writes the contribution paths and the steps. The walkthrough matches. About 8 hours.

1. Compute the walkthrough's sale on paper again: which lots sell what fraction, on your path and on the benchmark.
2. You write the sale fraction: shares sold over shares held just before the sale, applied to every lot of that symbol and to every fund lot that came from it. Proceeds are cash. Wire `refuse-oversell`.
3. You write the signatures for the two contribution paths, the three steps, and the headline. The agent fills them in.
4. Contributions are the deposits when the file has any, otherwise each purchase on its date.
5. Fund on your dates buys one fund lot per contribution on its date, at the next close within 7 days when the market was shut, and holds. All on day one buys the same lots on the first contribution date. Neither path ever sells.
6. Run `walkthrough` until every field matches. Then `weekend-deposit` and `as-of-input`. Then confirm `buys-only` and `half-cent` match every field.
7. Run the gate.

Done when `walkthrough`, `weekend-deposit`, `as-of-input`, and `refuse-oversell` pass, `buys-only` and `half-cent` match every field, and the gate passed.

Stop. If sales are still red on Sunday, write the failure at the top of the spec and carry it into next week. Buys-only is the demo fallback. Never make a sale consume one chosen lot.

### 14–20 December — distributions and a bare page

Dividends and distributions, the page path proven end to end, and the real closes downloaded. You write the signatures, and the agent writes the rest. About 7 hours.

1. You write the signatures where your replay takes a dividend and where a fund lot is paid a distribution. The agent fills them in.
2. A symbol's dividends are cash on its real path and one trace line under that symbol.
3. Every fund path is paid the fund's distributions, inside each lot's single rounding.
4. Run `dividend-quantity`, `interest-and-tax`, `dividend-then-buy`, `dividend-and-distribution`, and `self-holding`.
5. The Java program takes one case folder and writes the report as JSON on stdout.
6. A bare `web/` page: a Next.js route runs the jar on one fixture with `execFile` and prints the JSON. No styling.
7. Choose the demo's three comparison funds: Canadian-dollar funds that pay distributions and show no split in the source's split history over the demo's dates.
8. An agent writes `scripts/fetch-prices`. You run it once for the three funds over the demo's dates, and once for every symbol in the real export's slice, plus the funds you will compare it with, over its dates. Its output stays out of the chat, and the real export's files go in `exports/`. Use raw closes: in `yfinance`, for example, the default `Close` is already adjusted for dividends, so pass `auto_adjust=False`. Write the source and the date into each `SOURCES.md`.
9. Run the gate.

Done when all 23 fixtures pass with `pending.txt` empty, one browser request returns a report Java produced, the closes and distributions are downloaded, and the gate passed.

Stop. No styling, no upload, and no fund picker yet.

### 21–27 December — buffer

Nothing is planned.

1. If any fixture is red, fix it.
2. If everything is green, take the week off.

Done when every fixture is green, or each red one is written at the top of the spec.

Stop. No new rule and no new scope. If December exams land on 30 November to 20 December, move this buffer to the heaviest exam week and do that week's work over the holiday instead.

### 28 December – 3 January — Python

Python reproduces every fixture from the spec alone. An agent writes it, in its own chat, on a different model from the one that wrote any Java. About 5 hours.

1. For this week, add `java/` to `.cursorignore`. Take it out when the week is done.
2. Start a new chat on a different model. Give it this file, the spec, and the fixtures. It never sees the Java code or a Java diff. Neither does the chat that reviews its pull requests.
3. It writes `python/verifier/` with its own CSV reader. Every value is a `fractions.Fraction` built from the CSV's text: `Fraction("185.8904")`, never `Fraction(185.8904)`, which carries the float's binary error. `round()` on a `Fraction` rounds half to even, which is the spec's rule.
4. It emits the report's shape from the spec, key for key.
5. It runs every fixture with pytest, and writes `scripts/compare`.
6. You write a `run.json` for the real export's slice in `exports/`, then run compare on it yourself, outside the chat, with the closes from 14–20 December. Agreement is the check.
7. Read the verifier until you can say where its structure differs from the Java. Then run the gate.

Done when pytest passes on all 23 fixtures, compare agrees on every one, the real export runs through both engines, and the gate passed.

Stop. The Python chat never reads the Java code. Do not start the generator yet.

### 4–10 January — generated ledgers and CI

Both engines on generated ledgers, every field compared, and CI enforcing it. The agent writes the code. You choose the globs and diagnose every failure. About 7 hours, more if the generator finds a lot.

1. The agent lets the Java program take a folder of case folders, so one JVM start runs hundreds of ledgers.
2. The agent writes the Hypothesis strategy described in Checks, including the half-cent lots, and turns each of your invariant sentences into code. Confirm each one says what your sentence says.
3. The harness writes each ledger as a case, runs both engines, compares every field, and checks every invariant.
4. When it fails, you diagnose first: which rule broke, and in which engine. Save the shrunk ledger as a fixture, compute its answer on paper, and fix the engine that is wrong. You fix your files. The agent fixes its own once your diagnosis is written, and a Python fix goes through the Python chat. Add an entry to `docs/mistakes.md`.
5. The agent adds the formatters and linters in Stack. You run the formatters once over everything, in their own pull request.
6. CI grows to run, on every push, the format and lint checks, `./gradlew test`, pytest, compare on the fixtures, and 200 generated ledgers. All of them must pass before a pull request can merge.
7. You choose the money-path globs. The agent adds `actions/labeler` with them, and runs the long generated suite from a `paths` filter on the same globs.
8. Run both planted-bug checks from Engines, then restore them.
9. Open two test pull requests. One that edits `spec/ANALYTICS.md` must get `money-path` and the long run. One that edits only the README must get neither.
10. Write the decision record for comparing the engines on generated ledgers.
11. Run the gate.

Done when CI passes on `main`, the long run passes, both planted bugs were caught, every failure the generator found is now a fixture, and the gate passed.

Stop. No page work this week.

### 11–17 January — the page

The demo history, then one page that shows Java's report. You decide what the demo shows, and the agent builds it. About 6 hours.

1. You list the demo history's events and numbers: a sale, a clear drag, a holding of the comparison fund, a weekend deposit, a dividend, and a fund distribution. The agent types it into `demo/` as a case in the real export's shape, with made-up closes for its stocks and the downloaded closes for its funds. `demo/SOURCES.md` says which are which. Both engines must agree on it.
2. Upload one activities CSV. The page copies it into a new case folder in the system's temporary directory, never inside the repository, with the price and distribution files from `demo/`. Java reads it without `run.json`, and the transaction table lists every activity in the report.
3. The fund picker lists the funds that have closes in that price file. Choosing a fund and an as-of date writes `run.json` and runs Java again.
4. Show the headline, then the bridge, then the pick impacts. The best pick and the largest drag come from the report. A refusal shows the error's message and the fields it names, and nothing else.
5. Opening a symbol shows its trace. The lines sum to its impact.
6. Format cents only in `web/src/money.ts`, with a test for zero, a negative amount, and an amount over a million dollars.
7. Change one number in a fixture, rerun, and confirm the page changed only because the report changed.
8. Run the gate.

Done when someone else can upload the demo file, pick a fund, and read the bridge without help, and the gate passed.

Stop. No second copy of the math in the page. No replacement control and no assistant.

### 18–24 January — cold run

A cold run, a README, the likely questions, and a 90-second backup. All of it is yours. About 11 hours.

1. From a fresh clone of `main`, run compare, open the page, and do the three-minute core path with a timer.
2. Fix only a break in that path. A new idea goes on a list, not into the code.
3. Rewrite `README.md` for reviewers, in the words you will say. Open it with a map for the review: the diagram, the five check layers, and links to the decision records and one money-path pull request. How to build the jar and run Java, compare, the generator, and the page. What the bridge means. Prices were downloaded once. The value will not match the live Wealthsimple app. There is no assistant and no replacement control. Keep its link to this file. Each tradeoff links to its decision record. An agent may draft the commands. The rest is yours.
4. Add the questions a reviewer is likely to ask to `docs/questions.md`, with your answers: why not a percentage return; how this would run for every client; why the same fraction of every lot, which matches how Canada averages the cost of identical shares; why refuse instead of guess; why exact fractions; how you used agents; and what broke.
5. Record about 90 seconds of the core path.
6. Say the four tradeoffs out loud once.
7. Answer every question in `docs/questions.md` out loud, without notes.
8. Say the sentence at the end of Who writes what out loud, and check that every part of it is still true.

Done when you can do the core path without this file open, the README stands on its own, the recording exists, and you can answer every question in `docs/questions.md` without notes.

Stop. Do not restyle the page or add a fixture unless the cold run failed.

### 25–31 January — freeze

Rehearse twice. About 10 hours.

1. Rehearse the full 30 minutes on two different days, once with someone interrupting and asking what you wrote yourself.
2. If something breaks, use the rollback table. Return to the last green commit.
3. Read the README and the spec once. They should still describe the program you will open.
4. Leave replacement replay and the assistant unbuilt.
5. Tag `v1.0.0`. That is the demo.

Done when two full rehearsals are done, `v1.0.0` is tagged, and no rule changed during the week.

Stop. No new activity type, no new fund, and no live price call.

### February and March — reviews

The program stays at `v1.0.0`.

1. The week of a review, rehearse once and change nothing.
2. If a reviewer asks about something still red, show the spec and say so.
3. If they ask about the Wealthsimple balance, say the page values everything on the as-of date you chose, with downloaded closes, so it will not match the live app.
4. After a review, a fix is allowed only for something that failed in the room. It goes through a pull request like any other change and ships as `v1.0.1`.

Done when the demo you give is the demo you froze.

Stop. Do not start the assistant or the replacement control in the week of a review.

## Demo shape

Expect to be interrupted. The core path works on its own. The numbers come from a run on the synthetic history. The real export's slice appears only in the transaction table, to prove it parses, or as its refusal if no slice reads, and never with a real account id.

| Time | What you show |
| --- | --- |
| 0:00–0:30 | The demo CSV, then the transaction table. Nothing has been compared yet. |
| 0:30–1:00 | Pick the fund. The headline is your choices versus buying this fund on your own dates. |
| 1:00–2:00 | The bridge. When the money arrived, cash and sales, stock selection. The steps add up. |
| 2:00–3:00 | Open the largest drag. The lines sum to the impact. |

If they do not steer: the core path, then the architecture, then who wrote what, then the five check layers, then one entry from `docs/mistakes.md`, then the four tradeoffs, then their questions. For who wrote what, use the sentence at the end of Who writes what, then open one money-path pull request: the issue, the diff, the review, and the checks.

Show nothing you cannot explain two whys deep. Whatever you show is what they will ask about.

The four tradeoffs, each with its decision record. Stop at four.

1. The broker's cash is the truth. Shares times price is only a check.
2. Sell the same fraction of every lot.
3. One bridge whose steps add up.
4. Compare the engines on generated ledgers.

The line that carries the review: agents wrote most of the code, but the fixtures' answers were computed by hand, a second engine written separately has to agree with the first on ledgers nobody wrote, and the page shows the trades behind every number.

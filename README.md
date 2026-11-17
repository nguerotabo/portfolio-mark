# Portfolio Mark

Portfolio Mark answers one question about a brokerage history: did your choices beat simply buying a fund you pick, on your own dates, and which choices made the difference?

You upload one Wealthsimple activities file, pick one comparison fund, and pick an as-of date. A Java program replays the history and writes a report in cents. The page shows that report.

## What you see

The same money is valued four ways, all on the date you picked:

1. **All on day one.** Every contribution buys the fund on the first contribution date and is held.
2. **Fund on your dates.** Each contribution buys the fund on the day it arrived and is held.
3. **Benchmark.** Each purchase's dollars buy the fund that day. Each sale sells the same fraction of those fund shares. Cash from a sale stays cash.
4. **Your account.** What the file shows: your shares at the as-of close, plus cash.

The page is one bridge. Each step is the gap between two neighbouring paths, and the steps add up:

1. **When the money arrived.** Fund on your dates minus all on day one.
2. **Cash and sales.** Benchmark minus fund on your dates.
3. **Stock selection.** Your account minus the benchmark. This equals the sum of each holding's impact.

The **headline** is cash and sales plus stock selection: your choices versus buying the fund on your own dates. Opening a symbol shows that holding's lots on both paths, plus dividends. Those lines add up to its impact.

## How the numbers stay checkable

- Java produces every number. Python is a second implementation of the same written spec, written without reading the Java.
- Small cases have answers computed by hand. Both programs have to match those cents.
- A generator builds many ledgers nobody wrote by hand. The two programs have to agree, and a few rules have to hold on every one: the steps add up, and the pick impacts sum to stock selection.
- Closes and distributions are downloaded once and saved with their source and date. A run reads those files.
- The result will differ from the live Wealthsimple app. The page values the file on the date you chose, using saved closes.

## Stack

Java 21 (Gradle, JUnit) computes the report. A React page displays it. Python checks the same cases. GitHub Actions runs the tests and the comparison.

The rules live in [spec/ANALYTICS.md](spec/ANALYTICS.md). Scope and workflow live in [plan.md](plan.md).

## This version

One page, on your machine, because it reads brokerage files. A withdrawal, a second account, a second currency, or cash that goes below zero stops the run and names why. There is no database and no assistant.

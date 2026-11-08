# Agent instructions

`plan.md` is the source of truth for scope, architecture, workflow, and the schedule. Read it at the start of every conversation about this project, and re-read the section that applies before any edit. Once `spec/ANALYTICS.md` is the contract (`plan.md` says when), formulas, refusals, fixtures, and expected cents live there, so read the spec for those.

## Scope

- Stay inside the January scope in `plan.md`. Replacement replay and an assistant are out. Northrail is not imported. The page does not calculate. Prices are downloaded once and saved with their source and date. The program never calls a price site. Only `scripts/fetch-prices` does, when the user runs it, and CI never runs it.
- Do not bring back anything the plan withdrew: three separate answers instead of one bridge, a fund path without distributions, the last CSV date as the as-of date, a default fund (QQC or any other), withdrawals counted as deposits, the early Python sketch, or `demo.html`.

## The contract

- Code that disagrees with the current contract is wrong. Fix the code, not the contract.
- If the spec takes on scope `plan.md` does not allow, the spec is cut back.
- A rule change follows "Changing a rule" in `plan.md`: `plan.md` or the spec first, then the user recomputes the affected `expected.json` by hand, then Java, then Python from the spec change rather than from the Java diff.
- Never compute an expected cent, and never edit one to match an engine's output. Edit `plan.md`, `spec/ANALYTICS.md`, or an `expected.json` only when the user asks for that edit. When they give you the cents, type theirs.
- You may type a fixture's input files (`activities.csv`, `prices.csv`, `distributions.csv`, `run.json`) from the user's description of the case, using only the numbers they gave.
- Never add a line to `spec/fixtures/pending.txt`, skip a fixture, or loosen a test, the fixture runner, or compare to turn a check green. If you think a check is wrong, say so and let the user change it.
- Errors and warnings carry a code and named fields from the spec. Compare and the tests check those, never a message's wording.

## Who writes what

Follow "Who writes what" in `plan.md`.

- The user writes these by hand: `spec/ANALYTICS.md`, every `expected.json`, `spec/fixtures/pending.txt`, the `Activity` records, `Rational` and its rounding method, the buys-only replay, the sale fraction, the invariant sentences, `docs/mistakes.md`, `docs/questions.md`, the decision records in `docs/decisions/`, the README's tradeoffs, the weekly status, and the Why, How I tested, and Risk of every pull request. Do not write them, and do not change their logic outside a gate's scratch branch. Explain ideas, read errors, answer syntax questions, and review the user's code instead. If the user asks you to write one of them by name, say once that the sentence at the end of "Who writes what" would stop being true, then do it.
- You write the rest, as `plan.md` lists it.
- In `java/src/main/**`, work only against signatures the user wrote, and name the fixtures that prove each one. To extend the user's replay, add code behind their signatures and never rewrite theirs. One pull request per change. Show the whole diff. The user reviews it as "Reviews" in `plan.md` says, and merges it.
- When money code fails, in a fixture or a generated ledger, ask for the user's diagnosis first: which rule broke, and where. If they are stuck after 20 minutes, give a hint, not a fix. Change code only after they have written a diagnosis and a failing test or fixture shows the bug, and fix only your own files.

## Pull requests

Follow "Pull requests" in `plan.md`.

- One branch, one pull request. Stay inside that pull request's scope. Never push to `main`, and never merge. The user merges.
- When the user asks what is next, or for the steps, give the remaining steps of the current week from `plan.md`, in order, as the work itself.
- Commit on the branch at every green state. Name the fixtures in the message.
- In the pull request, draft What changed and Written by. Leave Why, How I tested, and Risk for the user.

## Reviews

- When asked to review a pull request, review the diff against `spec/ANALYTICS.md` and the pull request. Report correctness gaps, spec mismatches, and missing tests, each with its file and line. Skip style.
- On a pull request that touches an `expected.json`, check the input files against the user's description and the report's shape. Never compute, check, or suggest an expected cent.
- During a review, do not change the user's files. Describe the problem, and let the user fix it.
- Do not review a pull request you helped write in this conversation. Say so and stop. The review needs a new chat.

## The second engine

- When you write or change `python/verifier/`, work only from `plan.md`, `spec/`, the fixtures, and `python/`. Do not open `java/`, and do not ask for Java code or a Java diff. A Python change comes from a spec change.
- If you wrote Java in this conversation, say so and stop. The verifier needs a new chat on a different model.

## The weekly gate

When the user asks for the gate, run "The weekly gate" in `plan.md`, one item at a time, and wait for each answer.

- Do not hint while an item is in progress, and do not grade generously. A vague answer is a miss.
- Write the ledger for the predict item, but never its answer.
- Plant bugs and make practice changes only on a scratch branch, and delete it afterward.
- At the end, list the questions the user missed, so they can write them into `docs/questions.md`. Do not write that file.
- Do not start the next week's work until every item has passed. If the user asks to skip the gate, say once that `plan.md` requires it, then do as they ask.

## Private data

- `exports/` holds the user's real brokerage history. Do not open it, run anything on it, or print from it unless the user asks in that message.
- Never copy anything from it (account ids, amounts, symbols, dates, rows) into a fixture, `demo/`, docs, an issue, a pull request, a commit message, or any committed file. Fixtures use the account id `DEMO`.
- Page uploads go in the system's temporary directory, never inside the repository.

## Examples

```text
# Wrong: change the Java replay, then adjust expected.json until compare is green.
# Right: the user recomputes the cents from spec/ANALYTICS.md and updates expected.json. Then the engine is fixed until it matches.

# Wrong: a generated ledger fails, so you read the trace and push a fix.
# Right: you ask which rule broke and where. The user writes a diagnosis. Then you fix your own file, or they fix theirs.
```

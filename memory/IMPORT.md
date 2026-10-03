# 📥 Import Memory

> Last updated: [2026-10-03]

## Status: Bank statement (CSV) import — Working

Reachable as the **4th bottom tab "Import"** (`Destinations.IMPORT`).
Works in both `personalSms` and `safe` flavors — the `safe` flavor now has a
real capture path, which was a stated fallback requirement.

## Architecture
```
domain/csv/                    Pure Kotlin, unit-tested, no Android deps
├── Csv.kt                     Delimiter sniff, RFC4180 tokenizer, amount/date coercion
├── BankStatementParser.kt     Header detection, column mapping, row → StatementDraft
├── DescriptionHeuristics.kt   HDFC description → type / category / counterparty / reference
└── StatementDeduplicator.kt   Exact-hash + fuzzy dedup vs existing rows
data/csv/StatementImportManager.kt   Uri → preview() → commit() (single DB transaction)
feature/csvimport/             ViewModel + Compose screen (pick → preview → confirm)
```

## Why it is format-agnostic
The same code handles an HDFC **savings** statement (`Date, Description, Ref No,
Debit, Credit, Balance`) and an HDFC **credit card** statement
(`Date, Description, Amount, Type`). Column headers are fuzzy-matched onto 8
logical fields, so no per-bank parser is needed.

## Behaviours worth knowing
- **Delimiter** auto-detected from `, ; \t |`; BOM and CRLF handled.
- **Dates** are day-first (Indian convention). `04/03/2026` = 4 March, never 3 April.
- **Amounts** tolerate `₹1,23,456.78`, `Rs. 500`, `(500.00)` negatives, trailing `DR`.
- **Card bill payments are classified TRANSFER, not INCOME** — otherwise a credit-card
  payment would inflate monthly income. Salary/interest are matched *before* the
  generic "payment" rule so `SALARY PAYMENT` still counts as income.
- **Opening/closing balance rows are skipped.** Bare `total` is deliberately *not* a
  summary marker — a payee called "Total Fitness" would otherwise vanish.
- **Categories** are matched against seeded names; unknown ones are created on demand.
- **Duplicates** are detected two ways: exact `sourceHash` (re-importing the same file
  is a no-op) and a fuzzy same-day/same-amount/overlapping-token match that catches rows
  already captured from an SMS. Fuzzy matching errs toward *not* matching — dropping
  real spending is worse than showing a duplicate.
- Imported rows land **CONFIRMED** (a statement is authoritative), not in the review queue.
- Guard rails: 8 MB file cap, 20 000 row cap, UTF-8 / UTF-16 / Latin-1 decode fallback.

## Files added
- `data/db/dao/ImportRecordDao.kt` — **the entity existed but had no DAO**. Wired into
  `ExpenseTrackerDatabase`. No schema change, so still version 1.

## Tests
`BankStatementParserTest` — 22 cases: savings layout, credit-card layout, semicolon
delimiter, BOM+CRLF, quoted fields with embedded commas/newlines, day-first dates,
idempotent hashing, summary-row skipping, unreadable dates, zero amounts, totals.

## Not done / next
- No account auto-matching from the statement's masked number (the hint is parsed and
  shown, but the user still picks the target account).
- No column-mapping override UI if auto-detect fails.
- No XLS/XLSX support (HDFC net banking often exports XLS, not CSV).
- No statement-period grouping or per-row editing before commit.
# 🗄️ Database Schema Memory

> Last updated: [2026-10-03]

## Schema Version: 1

## Core Entities Created
- `AccountEntity` — bank, credit card, wallet, cash
- `TransactionEntity` — expense, income, transfer, refund
- `CategoryEntity` — with parent category support
- `MerchantEntity` — canonical name + aliases

- `TagEntity` + `TransactionTagCrossRef`
- `ImportRecordEntity` — SMS/CSV source tracking
- `CategorizationRuleEntity` — deterministic rule engine
- `BudgetEntity` — monthly per-category budgets

## Pending Entities
- `ReviewDecisionEntity` — before/after for rule improvement
- `BackupHistoryEntity` — backup metadata

## Key Indexes Created
- Unique on `sourceHash` in `TransactionEntity`
- `occurredAt`
- `(accountId, occurredAt)` composite
- `(categoryId, occurredAt)` composite
- Transaction `status` (review queue)

## Migration Log
| Version | Date | Change | Notes |
|---------|------|--------|-------|
| 1 | 2026-10-03 | Initial Creation | `Account`, `Transaction`, `Category`, `Merchant` created |

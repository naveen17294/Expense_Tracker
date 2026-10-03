# 🗄️ Database Schema Memory

> Last updated: [2026-10-03]

## Schema Version: Not yet created

## Planned Entities
- `AccountEntity` — bank, credit card, wallet, cash
- `TransactionEntity` — expense, income, transfer, refund
- `CategoryEntity` — with parent category support
- `MerchantEntity` — canonical name + aliases
- `TagEntity` + `TransactionTagCrossRef`
- `ImportRecordEntity` — SMS/CSV source tracking
- `CategorizationRuleEntity` — deterministic rule engine
- `BudgetEntity` — monthly per-category budgets
- `ReviewDecisionEntity` — before/after for rule improvement
- `BackupHistoryEntity` — backup metadata

## Key Indexes (Planned)
- Unique on normalized `sourceHash`
- UPI/bank reference + amount/account/date window
- `occurredAt DESC`
- `(accountId, occurredAt)` composite
- `(categoryId, occurredAt)` composite
- Normalized merchant/counterparty
- Transaction status (review queue)

## Migration Log
| Version | Date | Change | Notes |
|---------|------|--------|-------|
| — | — | — | No migrations yet |

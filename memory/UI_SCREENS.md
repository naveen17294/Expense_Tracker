# 🖥️ UI Screens & Navigation Memory

> Last updated: [2026-10-03]

## Design System — Rebuilt [2026-10-03]
Full MD3 rebuild. Location: `core/designsystem/`

| Token file | Contents |
|------------|----------|
| `theme/Color.kt` | Explicit light + dark tonal schemes (teal seed). Every surface/primary/inverse role filled in — no reliance on defaults. |
| `theme/Type.kt` | `AppTypography` — MD3 15-style scale, headings emphasised. `TextStyle.tabular()` for money columns. |
| `theme/Shape.kt` | `AppShapes` (6→30dp) + `Spacing` on the 8dp grid. |
| `theme/MoneyPalette.kt` | `MoneyPalette` derived from `ColorScheme` (not hardcoded) so `onX`/`X` pairing stays valid under dynamic colour. Exposed via `AppTheme.money`. |
| `theme/Theme.kt` | `ExpenseTrackerTheme` — dynamic colour supported but **off by default** so brand teal is consistent. |

### Reusable components
| Component | Notes |
|-----------|-------|
| `TransactionRow` | Leading category badge, title, type+category subtitle, signed tabular amount, source icon + date. Merged into one semantics node for TalkBack. |
| `AmountText` / `MoneyDirection` | Semantic colour + sign by flow direction. |
| `IconBadge` / `StatusPill` | Atoms for leading icons and small labels. |
| `CategoryVisuals` | Category name → icon + **stable** colour (derived from name hash, so it survives restarts). Falls back to `Icons.Rounded.Category`. |
| `EmptyState` | Shared icon + title + message + optional action. Replaces bare centred strings. |
| `SectionHeader` | Marked as a heading node for a11y navigation. |
| `AppSearchField` / `AmountField` / `QuickChoice` | Inputs and filter pills. |

## Navigation — Bottom Bar
| Tab | Screen | Status |
|-----|--------|--------|
| Home | Dashboard | ✅ Redesigned |
| Transactions | Transaction List | ✅ Redesigned |
| Review | Review Queue (badge count) | ✅ Redesigned |
| + FAB | Add/Edit Transaction | ✅ Redesigned |

Bottom bar and FAB animate in/out and only show on top-level destinations. Review tab shows a live badge from `MainActivity`.

## Secondary Screens
| Screen | Status |
|--------|--------|
| 4-Digit PIN Lock | ✅ Redesigned (haptics, shake-on-error, animated dots) |
| Backup & Restore | ✅ Redesigned (action cards, privacy warning) |
| Onboarding | ⬜ Not started |
| Account Management | ⬜ Not started |
| Category Management | ⬜ Not started |
| Search & Filters | 🟡 Basic (search + type chips only; no date/amount filters) |
| Import (SMS / CSV) | ⬜ Not started |
| Settings | ⬜ Not started |
| Transaction Detail | ⬜ Uses Add/Edit screen |
| Budgets / Reports | ⬜ Not started |

## Known Design Debt
- Dashboard `onAddTransactionClick` is unused; the FAB in `NavGraph` owns Add.
- `TransactionRow` shows the source date only; no relative "2h ago".
- No charts yet — `DECISIONS.md` defers the chart library choice to the reports phase.
- Dynamic colour is available but disabled; consider a settings toggle.
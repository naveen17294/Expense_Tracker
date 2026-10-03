# 🐛 Issues & Open Questions

> Last updated: [2026-10-03]

## Open Questions
- [ ] SMS permission feasibility on iQOO Z7s (Android 15) — needs testing on device
- [ ] Which chart library to use — Vico vs custom Canvas? (decide in reports phase)
- [ ] Should dynamic colour be user-toggleable instead of hard-off? (currently brand teal always)

## Resolved
| ID | Description | Resolution |
|----|-------------|------------|
| 1 | Leftover `com.example.expensetracker` template package | **Fixed [2026-10-03]** — deleted `app/src/{main,test,androidTest}/java/com/example`. It also contained a second `MainActivity` that risked conflicting with the real launcher activity. |
| 2 | `PermissionImpliesUnsupportedChromeOsHardware` lint error in `personalSms` manifest | **Fixed [2026-10-03]** — added `<uses-feature android:name="android.hardware.telephony" android:required="false" />` so the APK still installs on non-telephony devices for manual/CSV import. |
| 3 | `CategorizationEngineTest` failed to compile (`Double?` vs `Double`) | **Fixed [2026-10-03]** — `confidenceScore` is nullable on the entity. |
| 4 | `BankParsersTest` HDFC + ICICI failing | **Fixed [2026-10-03]** — the *assertions* were wrong, not the parser. They expected a trailing dot (`naveen@okicici.`) that the regex correctly excludes. Corrected to `naveen@okicici` / `NAVEEN`. |
| 5 | `FlowOperatorInvokedInComposition` in `MainActivity` | **Fixed [2026-10-03]** — review-count flow is now built inside `remember`. |

## Known Issues
| ID | Severity | Description | Status |
|----|----------|-------------|--------|
| 6 | Low | 38 lint warnings, all `GradleDependency`/`OldTargetApi` — newer library versions available | Open (informational) |
| 7 | Low | `DashboardScreen.onAddTransactionClick` is unused; the `NavGraph` FAB handles Add | Open |
| 8 | Low | Transaction search matches payee/note/amount only — no date, account, or amount-range filters yet | Open |
| 9 | Low | `AddEditTransactionUiState.isEditing()` infers edit-vs-create from field contents rather than the nav arg | Open |

## Blockers
- None currently
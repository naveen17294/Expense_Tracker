# 🏗️ Architecture & Design Decisions

> Last updated: [2026-10-03]

## Decision Log

### [2026-10-03] Initial Architecture Confirmed
- **Architecture:** Feature-oriented Clean Architecture, MVVM, UDF
- **Single module:** One Gradle app module with feature-oriented packages. Extract modules only if build time becomes a problem.
- **Database:** Room over SQLite. Compile-time SQL checking, migrations, Flow support.
- **DI:** Hilt
- **Async:** Kotlin Coroutines + Flow
- **State:** ViewModel + immutable UiState + StateFlow
- **Serialization:** Kotlinx Serialization
- **Date/Time:** `java.time`
- **Money:** `Long` paise (₹123.45 → 12345L). Never Double.
- **Background work:** WorkManager
- **Settings:** Preferences DataStore
- **Charts:** Vico (Compose-native) — evaluate during reports phase
- **Security:** 4-digit PIN lock, Android Keystore for local keys, AES-256-GCM for backups
- **Tests:** JUnit, Kotest assertions, MockK/fakes, Turbine, Room in-memory, Compose UI tests
- **Static analysis:** Detekt, Ktlint, Android Lint
- **CI:** GitHub Actions

### [2026-10-03] Package Structure
```
com.nk.expensetracker
├── app/          # Application, MainActivity, navigation
├── core/         # common, designsystem, money, time, security, testing
├── domain/       # model, repository, usecase, parser, categorization, reconciliation
├── data/         # db, datastore, sms, csv, backup, repository
├── feature/      # onboarding, dashboard, transactions, review, addedit, accounts, budgets, reports, search, backuprestore, settings
└── worker/       # WorkManager workers
```

### [2026-10-03] Build Flavors
- `personalSms` — SMS inbox scan + receiver + manual + CSV
- `safe` — Manual + CSV + optional notification listener

### [2026-10-03] No Backend
Room is single source of truth. Exported encrypted backup files provide portability. No cloud, no auth, no sync.

# ✅ Confirmed Requirements

> Last updated: [2026-10-03]

## User Profile
- **Name:** Naveen (nk)
- **Primary device:** iQOO Z7s — Android 15
- **Distribution:** Private sideload (not Play Store)

## Target Platform
- **targetSdk:** 35 (Android 15)
- **minSdk:** 34 (Android 14)
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3

## Package & Identity
- **Package name:** `com.nk.expensetracker`
- **App name:** Expense Tracker
- **GitHub repo:** `personal-expense-tracker` (private)

## Banks & UPI Apps (Parser Priority)
1. **Google Pay** (primary UPI app)
2. PhonePe
3. CRED
4. HDFC PayZapp
5. **Banks:** HDFC, SBI, ICICI, Axis
- User gets bank SMS for every UPI transaction

## Financial Defaults
- **Currency:** INR (₹), stored as Long paise
- **Month cycle:** Calendar month (1st to end-of-month)
- **Opening balances:** Not tracked — transaction-only tracking
- **Budget carryover:** Disabled

## Security
- **App lock:** 4-digit numeric PIN (not biometric — too complex for v1)
- **Backup encryption:** AES-256-GCM with passphrase-derived key
- **No internet permission**

## Build Flavors (from day one)
| Flavor | SMS Access | Fallback |
|--------|-----------|----------|
| `personalSms` | READ_SMS + RECEIVE_SMS | Manual + CSV |
| `safe` | No SMS | Manual + CSV + notification listener (optional) |

## Scope — v1.0.0
- All 6 phases (Foundation → Hardening)
- `release_v1.0.0` branch holds all completed work
- User decides per-feature which branch to use for deployment

## Explicit Non-Goals (v1)
- No cloud backend / sync
- No ML categorization
- No OCR receipts
- No investment/tax tracking
- No biometric auth (PIN only)
- No ads, analytics, or trackers

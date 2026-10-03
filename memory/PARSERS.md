# 📱 SMS Parsers Memory

> Last updated: [2026-10-03]

## Parser Registry Status

### Priority 1 — User's Primary Apps/Banks
| Bank/App | Template | Parser Status | Fixtures |
|----------|----------|--------------|----------|
| Google Pay (via bank SMS) | UPI debit | ⬜ Not started | ⬜ |
| Google Pay (via bank SMS) | UPI credit | ⬜ Not started | ⬜ |
| HDFC Bank | Debit SMS | 🟡 Beta | ⬜ |
| HDFC Bank | Credit SMS | 🟡 Beta | ⬜ |
| SBI | Debit SMS | 🟡 Beta | ⬜ |
| SBI | Credit SMS | 🟡 Beta | ⬜ |
| ICICI Bank | Debit SMS | 🟡 Beta | ⬜ |
| Axis Bank | Debit SMS | 🟡 Beta | ⬜ |

### Priority 2 — Secondary UPI Apps
| App | Template | Parser Status | Fixtures |
|-----|----------|--------------|----------|
| PhonePe | UPI notification | ⬜ Not started | ⬜ |
| CRED | Payment SMS | ⬜ Not started | ⬜ |
| HDFC PayZapp | Wallet SMS | ⬜ Not started | ⬜ |

### Negative Templates (Rejection)
| Type | Status | Fixtures |
|------|--------|----------|
| OTP messages | ⬜ Not started | ⬜ |
| Promotional SMS | ⬜ Not started | ⬜ |
| Failed transactions | ⬜ Not started | ⬜ |
| Balance inquiry | ⬜ Not started | ⬜ |

## Fixture Corpus
- Location: `app/src/test/resources/fixtures/`
- Total fixtures: 0
- Target: 30-50 varied, redacted message fixtures

## Notes
- User gets bank SMS for every UPI transaction (not app notifications)
- Google Pay is the primary UPI app
- Need redacted sample messages from user's actual SMS inbox

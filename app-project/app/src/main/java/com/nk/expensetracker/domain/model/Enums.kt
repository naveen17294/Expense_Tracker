package com.nk.expensetracker.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    REFUND
}

enum class TransactionStatus {
    DRAFT,
    NEEDS_REVIEW,
    CONFIRMED,
    EXCLUDED,
    DELETED
}

enum class TransactionSource {
    SMS,
    CSV,
    MANUAL,
    NOTIFICATION,
    RESTORED_BACKUP
}

package com.nk.expensetracker.core.money

import java.text.NumberFormat
import java.util.Locale

/**
 * All monetary values stored as Long paise (1/100 of a Rupee).
 * ₹123.45 = 12345L paise
 */
object Money {
    fun formatPaise(paise: Long): String {
        val rupees = paise / 100.0
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return formatter.format(rupees)
    }

    fun rupeesToPaise(rupees: Double): Long {
        return (rupees * 100).toLong()
    }

    fun paiseToRupees(paise: Long): Double {
        return paise / 100.0
    }
}

package com.nk.expensetracker.domain.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class BankStatementParserTest {

    private val ist = ZoneId.of("Asia/Kolkata")

    /** HDFC savings-style statement: title block, separate debit/credit columns. */
    private val hdfcSavingsStatement = """
        HDFC BANK LTD,Statement of Account
        Account No: XXXXXX1234,IFSC: HDFC0001234
        Statement Period: 01-Sep-2026 to 30-Sep-2026
        Date,Description,Ref No,Debit,Credit,Balance
        01-Sep-2026,"UPI/P2M/5239234/SWIGGY INSTAMART/HDFC BANK",5239234,250.00,,"1,25,000.50"
        02-Sep-2026,"IMPS/P2A/4127/NAVEEN KUMAR/HDFC BANK",4127,,"45,000.00","1,70,000.50"
        03-Sep-2026,"POS 5239 SWIGGY BANGALORE",,780.00,,"1,69,220.50"
        04-Sep-2026,"ATW 5239 CASH WITHDRAWAL",,2000.00,,"1,67,220.50"
        05-Sep-2026,"NEFT/NINAB/99881/SALARY OCT/HDFC BANK",99881,,"1,20,000.00","2,87,220.50"
        06-Sep-2026,"UPI/CR/77123/BILLPAY CREDIT CARD/HDFC",77123,,"15,000.00","3,02,220.50"
        07-Sep-2026,"DEBIT CARD PURCHASE REF 88912 AMAZON",88912,4999.00,,"2,97,221.50"
        Opening Balance,,,,,"1,25,250.50"
        Closing Balance,,,,,"2,97,221.50"
    """.trimIndent()

    @Test
    fun `detects comma delimiter and finds the header past the title block`() {
        val result = BankStatementParser.parse(hdfcSavingsStatement, ist)

        assertTrue(result.hasUsableHeader)
        assertEquals(',', result.delimiter)
        assertEquals(3, result.headerRowIndex)
        assertEquals(0, result.columnMapping[StatementField.DATE])
        assertEquals(3, result.columnMapping[StatementField.DEBIT])
        assertEquals(4, result.columnMapping[StatementField.CREDIT])
        assertEquals(2, result.columnMapping[StatementField.REFERENCE])
    }

    @Test
    fun `extracts the masked account number from the title block`() {
        val result = BankStatementParser.parse(hdfcSavingsStatement, ist)
        assertEquals("1234", result.accountHint)
    }

    @Test
    fun `parses every transaction row and skips balance summary lines`() {
        val result = BankStatementParser.parse(hdfcSavingsStatement, ist)

        assertEquals(7, result.drafts.size)
        assertEquals(
            "Opening and closing balance rows must not become transactions",
            2L,
            result.skipped[SkipReason.SUMMARY_ROW]?.toLong() ?: -1L
        )
    }

    @Test
    fun `reads debit rows as expenses with Indian digit grouping`() {
        val result = BankStatementParser.parse(hdfcSavingsStatement, ist)
        val swiggy = result.drafts.first()

        assertEquals(25_000L, swiggy.amountPaise)
        assertEquals("SWIGGY INSTAMART", swiggy.counterparty)
        assertEquals("5239234", swiggy.reference)
    }

    @Test
    fun `classifies a credit card bill payment as a transfer not income`() {
        val result = BankStatementParser.parse(hdfcSavingsStatement, ist)
        val billPayment = result.drafts.first { it.reference == "77123" }

        assertEquals(
            "A credit card payment must never inflate monthly income",
            com.nk.expensetracker.domain.model.TransactionType.TRANSFER,
            billPayment.type
        )
    }

    @Test
    fun `classifies salary credit as income`() {
        val result = BankStatementParser.parse(hdfcSavingsStatement, ist)
        val salary = result.drafts.first { it.description.contains("SALARY") }

        assertEquals(com.nk.expensetracker.domain.model.TransactionType.INCOME, salary.type)
        assertEquals("Salary", salary.suggestedCategoryName)
    }

    @Test
    fun `classifies cash withdrawal as an expense with the Cash category`() {
        val result = BankStatementParser.parse(hdfcSavingsStatement, ist)
        val atm = result.drafts.first { it.description.contains("CASH WITHDRAWAL") }

        assertEquals(com.nk.expensetracker.domain.model.TransactionType.EXPENSE, atm.type)
        assertEquals("Cash", atm.suggestedCategoryName)
    }

    @Test
    fun `extracts counterparty from an IMPS credit`() {
        val result = BankStatementParser.parse(hdfcSavingsStatement, ist)
        val imps = result.drafts.first { it.description.startsWith("IMPS") }

        assertEquals("NAVEEN KUMAR", imps.counterparty)
        assertEquals("4127", imps.reference)
    }

    @Test
    fun `reads the running balance column`() {
        val result = BankStatementParser.parse(hdfcSavingsStatement, ist)
        assertEquals(12_500_050L, result.drafts.first().balancePaise)
    }

    @Test
    fun `produces a stable hash so re-importing is idempotent`() {
        val first = BankStatementParser.parse(hdfcSavingsStatement, ist)
        val second = BankStatementParser.parse(hdfcSavingsStatement, ist)

        assertEquals(
            "Parsing the same file twice must yield identical hashes",
            first.drafts.map { it.sourceHash },
            second.drafts.map { it.sourceHash }
        )
    }

    @Test
    fun `hash differs when the amount differs`() {
        val base = BankStatementParser.parse(hdfcSavingsStatement, ist)
        val altered = hdfcSavingsStatement.replace("250.00,", "260.00,")

        val alteredResult = BankStatementParser.parse(altered, ist)
        assertFalse(base.drafts.first().sourceHash == alteredResult.drafts.first().sourceHash)
    }

    // ---- credit card layout: single amount column with a Dr/Cr indicator ----

    @Test
    fun `handles a credit card statement with a single amount and type column`() {
        val statement = """
            HDFC CREDIT CARD STATEMENT
            Card No: XXXX XXXX XXXX 4455
            Date,Description,Amount,Type
            05/09/2026,SWIGGY ORDER 4521,649.00,Debit
            06/09/2026,AMAZON PAYMENT,-12000.00,Credit
            07/09/2026,NETFLIX SUBSCRIPTION,649.00,Debit
        """.trimIndent()

        val result = BankStatementParser.parse(statement, ist)

        assertTrue(result.hasUsableHeader)
        assertEquals(2, result.columnMapping[StatementField.AMOUNT])
        assertEquals(3, result.columnMapping[StatementField.DIRECTION])
        assertEquals(3, result.drafts.size)

        val swiggy = result.drafts[0]
        assertEquals(64_900L, swiggy.amountPaise)
        assertEquals(com.nk.expensetracker.domain.model.TransactionType.EXPENSE, swiggy.type)

        val payment = result.drafts[1]
        assertEquals(1_200_000L, payment.amountPaise)
        assertEquals(
            com.nk.expensetracker.domain.model.TransactionType.TRANSFER,
            payment.type
        )
    }

    // ---- robustness ----

    @Test
    fun `handles semicolon separated files`() {
        val statement = """
            Date;Description;Debit;Credit;Balance
            03/10/2026;UPI/P2M/111/SWIGGY/ICICI;150.00;;900.00
        """.trimIndent()

        val result = BankStatementParser.parse(statement, ist)

        assertEquals(';', result.delimiter)
        assertEquals(1, result.drafts.size)
        assertEquals(15_000L, result.drafts.single().amountPaise)
    }

    @Test
    fun `handles a BOM and CRLF line endings`() {
        val statement =
            "\uFEFFDate,Description,Amount,Type\r\n01/10/2026,UPI/P2M/9/AMAZON/HDFC,99.50,Debit\r\n"

        val result = BankStatementParser.parse(statement, ist)

        assertEquals(1, result.drafts.size)
        assertEquals(9_950L, result.drafts.single().amountPaise)
    }

    @Test
    fun `keeps commas and newlines inside quoted descriptions`() {
        val statement = """
            Date,Description,Amount,Type
            01/10/2026,"POS 123, Merchant, City
            Second line",250.00,Debit
        """.trimIndent()

        val result = BankStatementParser.parse(statement, ist)

        assertEquals(1, result.drafts.size)
        assertEquals(25_000L, result.drafts.single().amountPaise)
        assertTrue(result.drafts.single().description.contains("Second line"))
    }

    @Test
    fun `reports a helpful failure for a file with no recognisable header`() {
        val result = BankStatementParser.parse("hello\nworld\nnothing useful here", ist)

        assertFalse(result.hasUsableHeader)
        assertTrue(result.drafts.isEmpty())
        assertTrue(result.issues.single().contains("No statement header"))
    }

    @Test
    fun `skips rows with an unreadable date rather than guessing`() {
        val statement = """
            Date,Description,Amount,Type
            not-a-date,UPI/P2M/1/FOO/HDFC,100.00,Debit
            02/10/2026,UPI/P2M/2/BAR/HDFC,200.00,Debit
        """.trimIndent()

        val result = BankStatementParser.parse(statement, ist)

        assertEquals(1, result.drafts.size)
        assertEquals(1, result.skipped[SkipReason.NO_DATE])
    }

    @Test
    fun `skips zero amount rows`() {
        val statement = """
            Date,Description,Amount,Type
            02/10/2026,MINIMUM BALANCE CHARGE,0.00,Debit
            03/10/2026,UPI/P2M/3/CAFE/HDFC,60.00,Debit
        """.trimIndent()

        val result = BankStatementParser.parse(statement, ist)

        assertEquals(1, result.drafts.size)
        assertEquals(1, result.skipped[SkipReason.ZERO_AMOUNT])
    }

    @Test
    fun `totals debits and credits separately`() {
        val result = BankStatementParser.parse(hdfcSavingsStatement, ist)

        // 250 + 780 + 2000 + 4999 = 8029.00 rupees
        assertEquals(802_900L, result.totalDebitPaise)
        // 45000 + 120000 salary only; the card payment is a transfer
        assertEquals(16_500_000L, result.totalCreditPaise)
    }

    // ---- primitives ----

    @Test
    fun `parses amounts written several ways`() {
        assertEquals(123_456_78L, paise("₹1,23,456.78"))
        assertEquals(50_000L, paise("Rs. 500"))
        assertEquals(-50_000L, paise("(500.00)"))
        assertEquals(-50_000L, paise("500.00 DR"))
        assertEquals(0L, paise("0"))
        assertNull(Csv.parseSignedPaise(""))
        assertNull(Csv.parseSignedPaise("N/A"))
    }

    private fun paise(raw: String): Long = requireNotNull(Csv.parseSignedPaise(raw)) { raw }

    @Test
    fun `parses day first dates`() {
        val millis = requireNotNull(Csv.parseDateToMillis("03/04/2026", ist))
        val zoned = java.time.Instant.ofEpochMilli(millis).atZone(ist)

        assertEquals(3, zoned.dayOfMonth)
        assertEquals(4, zoned.monthValue)
    }

    @Test
    fun `treats a leading number as a day when it can be one`() {
        val millis = requireNotNull(Csv.parseDateToMillis("04/03/2026", ist))
        val zoned = java.time.Instant.ofEpochMilli(millis).atZone(ist)

        // Indian statements are day-first, so 04/03 is 4 March, not 3 April.
        assertEquals(4, zoned.dayOfMonth)
        assertEquals(3, zoned.monthValue)
    }
}
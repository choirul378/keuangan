package com.example.ui.components

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyUtils {
    private val localeId = Locale("id", "ID")
    private val currencyFormatter = NumberFormat.getNumberInstance(localeId)

    fun formatRupiah(amount: Long, includePrefix: Boolean = true): String {
        val formatted = currencyFormatter.format(amount)
        return if (includePrefix) "Rp $formatted" else formatted
    }

    fun formatCompact(amount: Long): String {
        return when {
            amount >= 1_000_000_000L -> String.format(localeId, "%.1f M", amount / 1_000_000_000.0)
            amount >= 1_000_000L -> String.format(localeId, "%.1f jt", amount / 1_000_000.0)
            amount >= 1_000L -> String.format(localeId, "%.0f rb", amount / 1_000.0)
            else -> amount.toString()
        }
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", localeId)
        return sdf.format(Date(millis))
    }

    fun formatDateFull(millis: Long): String {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", localeId)
        return sdf.format(Date(millis))
    }

    fun formatMonthYear(year: Int, monthZeroIndexed: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, year)
        cal.set(java.util.Calendar.MONTH, monthZeroIndexed)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val sdf = SimpleDateFormat("MMMM yyyy", localeId)
        return sdf.format(cal.time)
    }

    fun formatShortMonth(monthZeroIndexed: Int): String {
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
        return months.getOrElse(monthZeroIndexed) { "" }
    }
}

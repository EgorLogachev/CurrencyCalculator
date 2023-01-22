package com.example.currencycalculator.data.utils

import java.text.SimpleDateFormat
import java.util.*

fun Calendar.getFormattedDate(): String = time.getFormattedDate()

fun Date.getFormattedDate(pattern: String? = null): String {
    val dateFormatter = if (pattern == null)
        SimpleDateFormat.getDateInstance()
    else
        SimpleDateFormat(pattern, Locale.getDefault())
    return  dateFormatter.format(this)
}
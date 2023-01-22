package com.example.currencycalculator.data

data class Conversion(
    val originalAmount: Float,
    val originalCurrency: Currency,
    val targetAmount: Float,
    val targetCurrency: Currency
)
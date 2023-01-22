package com.example.currencycalculator.data

data class Conversion(
    private val originalAmount: Float,
    private val originalCurrency: Currency,
    private val targetAmount: Float,
    private val targetCurrency: Currency
)
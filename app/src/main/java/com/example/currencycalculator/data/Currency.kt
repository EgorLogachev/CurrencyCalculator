package com.example.currencycalculator.data

import com.google.gson.annotations.SerializedName

data class Currency(
    @SerializedName("txt")
    var text: String?,
    @SerializedName("rate")
    var rate: Float?,
    @SerializedName("cc")
    var code: String?,
)
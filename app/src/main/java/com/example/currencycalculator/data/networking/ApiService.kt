package com.example.currencycalculator.data.networking

import com.example.currencycalculator.data.Currency
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("NBUStatService/v1/statdirectory/exchangenew?json&")
    suspend fun getCurrencies(@Query("date") date: String): Response<List<Currency>>

}
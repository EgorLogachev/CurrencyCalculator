package com.example.currencycalculator.data

import com.example.currencycalculator.data.networking.ApiService
import com.example.currencycalculator.data.storage.ConversionsStorage
import com.example.currencycalculator.data.utils.getFormattedDate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

interface Repository {
    suspend fun getCurrencies(date: Date): Result<List<Currency>>

    suspend fun getHistory(): Result<List<Conversion>>

    suspend fun updateHistory(conversion: Conversion? = null): Result<List<Conversion>>
}

class RepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val storage: ConversionsStorage
) : Repository {

    companion object {
        const val DATE_FORMAT = "yyyyMMdd"
    }

    override suspend fun getCurrencies(date: Date): Result<List<Currency>> {
        return withContext(Dispatchers.IO) {
            val response = apiService.getCurrencies(date.getFormattedDate(DATE_FORMAT))
            response.run {
                return@run when {
                    isSuccessful -> Result.success(
                        mutableListOf(
                            Currency("Українська гривня", 1f, "UAH")
                        ).apply { body()?.let { addAll(it) } }.toList()
                    )
                    else -> Result.failure(
                        Exception(
                            errorBody()?.string() ?: "Currencies fetching error"
                        )
                    )
                }
            }
        }
    }

    override suspend fun getHistory() = updateHistory()

    override suspend fun updateHistory(conversion: Conversion?): Result<List<Conversion>> {
        return withContext(Dispatchers.IO) {
            conversion?.let {
                storage.add(conversion)
            }
            Result.success(storage.history)
        }

    }

}

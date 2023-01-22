package com.example.currencycalculator.data.storage

import android.content.Context
import com.example.currencycalculator.data.Conversion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import javax.inject.Inject

interface ConversionsStorage {

    val history: List<Conversion>

    fun add(conversion: Conversion) : Boolean
}

class ConversionsStorageImpl @Inject constructor(context: Context) : ConversionsStorage {
    companion object {
        const val PREFERENCES_NAME = "com.example.currencycalculator.Storage"
        const val HISTORY_KEY = "history"
        const val HISTORY_SIZE = 10
    }

    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _history: LinkedList<Conversion> by lazy {
        prefs.getString(HISTORY_KEY, null)?.let {
            val collectionType = object : TypeToken<LinkedList<Conversion?>>() {}.type
            return@let gson.fromJson(it, collectionType)
        } ?: LinkedList()
    }

    override val history: List<Conversion> = _history

    override fun add(conversion: Conversion): Boolean {
        return with(_history) {
            if (isNotEmpty() && first == conversion) return@with false
            addFirst(conversion)
            while (size > HISTORY_SIZE) {
                removeLast()
            }
            saveHistory()
            return@with true
        }
    }

    private fun saveHistory() {
        prefs.edit().apply {
            putString(HISTORY_KEY, gson.toJson(_history))
        }.apply()
    }
}


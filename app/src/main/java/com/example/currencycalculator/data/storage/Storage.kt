package com.example.currencycalculator.data.storage

import android.content.Context

interface Storage {

}

class StorageImpl(context: Context) : Storage {
    companion object {
        const val PREFERENCES_NAME = "com.example.currencycalculator.Storage"
    }

    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}


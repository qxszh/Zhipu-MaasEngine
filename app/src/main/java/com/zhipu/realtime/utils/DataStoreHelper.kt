package com.zhipu.realtime.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object DataStoreHelper {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    private val API_KEY = stringPreferencesKey("apikey")

    suspend fun setApiKey(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = value
        }
    }

    fun getApiKey(context: Context): Flow<String?> {
        return context.dataStore.data
            .map { preferences ->
                preferences[API_KEY]
            }
    }
}
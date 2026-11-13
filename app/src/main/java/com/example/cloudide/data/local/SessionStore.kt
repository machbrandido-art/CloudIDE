package com.example.cloudide.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Non-secret session info (the GitHub username). */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val username: Flow<String?> = context.appDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it[KEY_USERNAME] }
        .distinctUntilChanged()

    suspend fun saveUsername(username: String) {
        context.appDataStore.edit { it[KEY_USERNAME] = username }
    }

    suspend fun clear() {
        context.appDataStore.edit { it.remove(KEY_USERNAME) }
    }

    private companion object {
        val KEY_USERNAME = stringPreferencesKey("username")
    }
}
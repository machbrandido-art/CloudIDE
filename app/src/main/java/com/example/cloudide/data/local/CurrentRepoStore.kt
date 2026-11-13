package com.example.cloudide.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.cloudide.domain.CurrentRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The repo the user is working on, plus the last few repos (for the drawer). */
@Singleton
class CurrentRepoStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: Flow<Preferences> = context.appDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    val current: Flow<CurrentRepo?> = prefs
        .map { decode(it[KEY_CURRENT]) }
        .distinctUntilChanged()

    val recents: Flow<List<CurrentRepo>> = prefs
        .map { readRecents(it) }
        .distinctUntilChanged()

    suspend fun select(repo: CurrentRepo) {
        context.appDataStore.edit { p ->
            p[KEY_CURRENT] = encode(repo)
            val updated = listOf(repo) + readRecents(p).filter { it.fullName != repo.fullName }
            p[KEY_RECENTS] = updated.take(MAX_RECENTS).joinToString("\n") { encode(it) }
        }
    }

    suspend fun clear() {
        context.appDataStore.edit {
            it.remove(KEY_CURRENT)
            it.remove(KEY_RECENTS)
        }
    }

    private fun readRecents(p: Preferences): List<CurrentRepo> =
        p[KEY_RECENTS].orEmpty().lines().mapNotNull { decode(it) }

    // Tab separated: tabs cannot appear in GitHub logins, repo names or branch names.
    private fun encode(repo: CurrentRepo): String =
        "${repo.owner}\t${repo.name}\t${repo.defaultBranch}"

    private fun decode(value: String?): CurrentRepo? {
        if (value.isNullOrBlank()) return null
        val parts = value.split('\t')
        if (parts.size != 3 || parts.any { it.isBlank() }) return null
        return CurrentRepo(owner = parts[0], name = parts[1], defaultBranch = parts[2])
    }

    private companion object {
        const val MAX_RECENTS = 5
        val KEY_CURRENT = stringPreferencesKey("current_repo")
        val KEY_RECENTS = stringPreferencesKey("recent_repos")
    }
}
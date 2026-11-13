package com.example.cloudide.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** The single DataStore instance of the app. Must stay a top-level property. */
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "cloudide_prefs")
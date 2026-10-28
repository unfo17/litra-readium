package io.github.unfo17.litra.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore("settings")
data class UserPreferences(val appearance: String = "SYSTEM", val listView: Boolean = false, val sort: String = "RECENT", val keepAwake: Boolean = true, val rememberSearch: Boolean = true, val readerTheme: String = "SEPIA", val fontSize: Double = 1.0, val lineHeight: Double = 1.6, val margins: Double = 1.0, val justify: Boolean = false, val scroll: Boolean = false, val fontFamily: String = "SERIF", val brightness: Double = -1.0)
@Singleton
class PreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {
    val preferences = context.settingsStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { p ->
        UserPreferences(p[stringPreferencesKey("appearance")] ?: "SYSTEM", p[booleanPreferencesKey("list")] ?: false, p[stringPreferencesKey("sort")] ?: "RECENT", p[booleanPreferencesKey("awake")] ?: true, p[booleanPreferencesKey("history")] ?: true, p[stringPreferencesKey("readerTheme")] ?: "SEPIA", p[doublePreferencesKey("font")] ?: 1.0, p[doublePreferencesKey("line")] ?: 1.6, p[doublePreferencesKey("margins")] ?: 1.0, p[booleanPreferencesKey("justify")] ?: false, p[booleanPreferencesKey("scroll")] ?: false, p[stringPreferencesKey("fontFamily")] ?: "SERIF", p[doublePreferencesKey("brightness")] ?: -1.0)
    }
    suspend fun text(key: String, value: String) { context.settingsStore.edit { it[stringPreferencesKey(key)] = value } }
    suspend fun flag(key: String, value: Boolean) { context.settingsStore.edit { it[booleanPreferencesKey(key)] = value } }
    suspend fun number(key: String, value: Double) { context.settingsStore.edit { it[doublePreferencesKey(key)] = value } }
}

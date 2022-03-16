package mega.privacy.android.app.data.gateway

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.logging.KARERE_LOGS
import mega.privacy.android.app.logging.LOG_PREFERENCES
import mega.privacy.android.app.logging.SDK_LOGS
import java.io.IOException
import javax.inject.Inject

private val Context.loggingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = LOG_PREFERENCES,
    produceMigrations = {
        listOf(
            SharedPreferencesMigration(it, LOG_PREFERENCES)
        )
    })

class LoggingSettingsGateway @Inject constructor(@ApplicationContext private val context: Context) {
    private val logPreferenceKey = booleanPreferencesKey(SDK_LOGS)
    private val chatLogPreferenceKey = booleanPreferencesKey(KARERE_LOGS)

    fun isLoggingEnabled(): Flow<Boolean> =
        context.loggingDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                preferences[logPreferenceKey] ?: false
            }

    suspend fun setLoggingEnabled(enabled: Boolean) {
        context.loggingDataStore.edit { it[logPreferenceKey] = enabled }
    }

    fun isChatLoggingEnabled(): Flow<Boolean> =
        context.loggingDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                preferences[chatLogPreferenceKey] ?: false
            }

    suspend fun setChatLoggingEnabled(enabled: Boolean) {
        context.loggingDataStore.edit { it[chatLogPreferenceKey] = enabled }
    }

}
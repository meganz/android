package mega.privacy.android.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.preferences.LoggingPreferencesGateway
import mega.privacy.android.app.di.IoDispatcher
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

class LoggingPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LoggingPreferencesGateway {
    private val logPreferenceKey = booleanPreferencesKey(SDK_LOGS)
    private val chatLogPreferenceKey = booleanPreferencesKey(KARERE_LOGS)

    override fun isLoggingPreferenceEnabled(): Flow<Boolean> =
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

    override suspend fun setLoggingEnabledPreference(enabled: Boolean) {
        withContext(ioDispatcher) {
            context.loggingDataStore.edit {
                it[logPreferenceKey] = enabled
            }
        }
    }

    override fun isChatLoggingPreferenceEnabled(): Flow<Boolean> =
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

    override suspend fun setChatLoggingEnabledPreference(enabled: Boolean) {
        withContext(ioDispatcher) {
            context.loggingDataStore.edit {
                it[chatLogPreferenceKey] = enabled
            }
        }
    }

}
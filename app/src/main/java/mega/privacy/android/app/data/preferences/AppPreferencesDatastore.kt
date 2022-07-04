package mega.privacy.android.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.data.gateway.preferences.AppPreferencesGateway
import mega.privacy.android.app.di.IoDispatcher
import org.jetbrains.anko.defaultSharedPreferences
import java.io.IOException
import javax.inject.Inject

private val Context.appPreferencesDatastore: DataStore<Preferences> by preferencesDataStore(

    name = "app_preferences",
    produceMigrations = {
        listOf(
            SharedPreferencesMigration(
                produceSharedPreferences = it::defaultSharedPreferences,
                keysToMigrate = setOf(SettingsConstants.KEY_APPEARANCE_COLOR_THEME)
            )
        )
    })

/**
 * App preferences datastore, implements [AppPreferencesGateway]
 *
 * @property context
 * @property ioDispatcher
 */
class AppPreferencesDatastore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppPreferencesGateway {

    override suspend fun putString(key: String, value: String) =
        putPreference(stringPreferencesKey(key), value)

    override suspend fun putStringSet(key: String, value: MutableSet<String>) =
        putPreference(stringSetPreferencesKey(key), value)

    override suspend fun putInt(key: String, value: Int) =
        putPreference(intPreferencesKey(key), value)

    override suspend fun putLong(key: String, value: Long) =
        putPreference(longPreferencesKey(key), value)

    override suspend fun putFloat(key: String, value: Float) =
        putPreference(floatPreferencesKey(key), value)

    override suspend fun putBoolean(key: String, value: Boolean) =
        putPreference(booleanPreferencesKey(key), value)

    private suspend fun <T> putPreference(
        prefKey: Preferences.Key<T>,
        value: T,
    ) {
        withContext(ioDispatcher) {
            context.appPreferencesDatastore.edit {
                it[prefKey] = value
            }
        }
    }

    override fun monitorString(key: String, defaultValue: String?) =
        getPreferenceData().map {
            it[stringPreferencesKey(key)] ?: defaultValue
        }

    override fun monitorStringSet(
        key: String,
        defaultValue: MutableSet<String>?,
    ) = getPreferenceData().map {
        it[stringSetPreferencesKey(key)] ?: defaultValue
    }.map { it?.toMutableSet() }

    override fun monitorInt(key: String, defaultValue: Int) =
        monitorValue(intPreferencesKey(key), defaultValue)

    override fun monitorLong(key: String, defaultValue: Long) =
        monitorValue(longPreferencesKey(key), defaultValue)

    override fun monitorFloat(key: String, defaultValue: Float) =
        monitorValue(floatPreferencesKey(key), defaultValue)

    override fun monitorBoolean(key: String, defaultValue: Boolean) =
        monitorValue(booleanPreferencesKey(key), defaultValue)

    private fun <T> monitorValue(
        prefKey: Preferences.Key<T>,
        defaultValue: T,
    ) = getPreferenceData().map {
        it[prefKey] ?: defaultValue
    }

    private fun getPreferenceData() = context.appPreferencesDatastore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

}
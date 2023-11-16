package mega.privacy.android.data.preferences.environment

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.AppInfoPreferencesGateway
import javax.inject.Inject
import javax.inject.Named

internal const val APP_VERSION_CODE_KEY = "APP_VERSION_CODE"
internal const val IS_FIRST_LAUNCH_KEY = "IS_FIRST_LAUNCH"
internal const val appInfoPreferenceFileName = "APP_INFO"

/**
 * App info preferences datastore
 *
 */
internal class AppInfoPreferencesDatastore @Inject constructor(
    @Named(appInfoPreferenceFileName) private val dataStore: DataStore<Preferences>,
) : AppInfoPreferencesGateway {
    private val preferredAppVersionCodeKey =
        intPreferencesKey(APP_VERSION_CODE_KEY)

    private val isFirstLaunchKey = booleanPreferencesKey(IS_FIRST_LAUNCH_KEY)

    override suspend fun setLastVersionCode(versionCode: Int) {
        dataStore.edit {
            it[preferredAppVersionCodeKey] = versionCode
        }
    }

    override fun monitorLastVersionCode(): Flow<Int> = dataStore.monitor(preferredAppVersionCodeKey)
        .map { it ?: 0 }

    override suspend fun setIsFirstLaunch(isFirstLaunch: Boolean) {
        dataStore.edit { it[isFirstLaunchKey] = isFirstLaunch }
    }

    override fun monitorIsFirstLaunch(): Flow<Boolean?> = dataStore.monitor(isFirstLaunchKey)

}
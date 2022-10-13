package mega.privacy.android.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.data.gateway.preferences.AppInfoPreferencesGateway
import mega.privacy.android.app.utils.SharedPreferenceConstants
import java.io.IOException
import javax.inject.Inject

private const val appInfoPreferenceFileName = SharedPreferenceConstants.APP_INFO_FILE
private val Context.appInfoPreferenceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = appInfoPreferenceFileName,
    produceMigrations = {
        listOf(
            SharedPreferencesMigration(
                context = it,
                sharedPreferencesName = appInfoPreferenceFileName,
                keysToMigrate = setOf(
                    SharedPreferenceConstants.APP_VERSION_CODE_KEY,
                )
            ),
        )
    })

/**
 * App info preferences datastore
 *
 */
class AppInfoPreferencesDatastore @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppInfoPreferencesGateway {
    private val preferredAppVersionCodeKey =
        intPreferencesKey(SharedPreferenceConstants.APP_VERSION_CODE_KEY)

    override suspend fun setLastVersionCode(versionCode: Int) {
        context.appInfoPreferenceDataStore.edit {
            it[preferredAppVersionCodeKey] = versionCode
        }
    }

    override fun monitorLastVersionCode(): Flow<Int> = context.appInfoPreferenceDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map {
            it[preferredAppVersionCodeKey] ?: 0
        }

}
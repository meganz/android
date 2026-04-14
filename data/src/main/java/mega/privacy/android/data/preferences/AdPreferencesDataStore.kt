package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.gateway.preferences.AdPreferencesGateway
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AdPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdPreferencesGateway {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "AD_PREFERENCES",
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
    )

    private val rewardedAdAttemptCountKey = intPreferencesKey(REWARDED_AD_ATTEMPT_COUNT)

    override suspend fun incrementRewardedAdAttemptCount() {
        context.dataStore.edit { prefs ->
            prefs[rewardedAdAttemptCountKey] = (prefs[rewardedAdAttemptCountKey] ?: 0) + 1
        }
    }

    override suspend fun resetRewardedAdAttemptCount() {
        context.dataStore.edit { it[rewardedAdAttemptCountKey] = 0 }
    }

    override fun monitorRewardedAdAttemptCount(): Flow<Int> =
        context.dataStore.data.map { it[rewardedAdAttemptCountKey] ?: 0 }

    companion object {
        private const val REWARDED_AD_ATTEMPT_COUNT = "REWARDED_AD_ATTEMPT_COUNT"
    }
}

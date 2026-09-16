package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.PushesPreferencesGateway
import mega.privacy.android.data.qualifier.PushesPreference
import javax.inject.Inject

/**
 * Default Implementation of [PushesPreferencesGateway]
 */
internal class PushesPreferencesDataStore @Inject constructor(
    @PushesPreference private val dataStore: DataStore<Preferences>,
) : PushesPreferencesGateway {

    override suspend fun getPushToken(): String =
        dataStore.monitor(KEY_NEW_TOKEN).firstOrNull() ?: ""

    override suspend fun setPushToken(newToken: String) {
        dataStore.edit { it[KEY_NEW_TOKEN] = newToken }
    }

    override suspend fun clearPushToken() {
        dataStore.edit { it.clear() }
    }

    companion object {

        /**
         * String Preference Key for the push token
         */
        private val KEY_NEW_TOKEN = stringPreferencesKey("NEW_TOKEN")

        /**
         * DataStore File Name
         */
        const val PUSH_TOKEN_FILE = "PUSH_TOKEN"
    }
}

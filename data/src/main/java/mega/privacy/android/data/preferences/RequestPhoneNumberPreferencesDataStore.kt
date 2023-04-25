package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.RequestPhoneNumberPreferencesGateway
import mega.privacy.android.data.qualifier.RequestPhoneNumberPreference
import javax.inject.Inject

/**
 * Default Implementation of [RequestPhoneNumberPreferencesGateway]
 */
internal class RequestPhoneNumberPreferencesDataStore
@Inject constructor(@RequestPhoneNumberPreference private val dataStore: DataStore<Preferences>) :
    RequestPhoneNumberPreferencesGateway {

    override suspend fun setRequestPhoneNumberPreference(isShown: Boolean) {
        dataStore.edit { it[KEY_REQUEST_PHONE_NUMBER] = isShown }
    }

    override suspend fun isRequestPhoneNumberPreferenceShown() =
        dataStore.monitor(KEY_REQUEST_PHONE_NUMBER).firstOrNull() ?: false


    companion object {

        /**
         * Boolean Preference Key for requesting the phone number
         */
        private val KEY_REQUEST_PHONE_NUMBER =
            booleanPreferencesKey("KEY_REQUEST_PHONE_NUMBER")

        /**
         * DataStore File Name
         */
        const val REQUEST_PHONE_NUMBER_FILE = "REQUEST_PHONE_NUMBER_FILE"
    }
}
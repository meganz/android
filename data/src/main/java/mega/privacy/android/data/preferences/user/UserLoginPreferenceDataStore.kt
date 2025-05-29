package mega.privacy.android.data.preferences.user

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.user.UserLoginPreferenceGateway
import javax.inject.Inject
import javax.inject.Named

internal const val userLoginPreferenceDataStoreName = "user_login_preference"

/**
 * DataStore implementation for user login preferences.
 */
internal class UserLoginPreferenceDataStore @Inject constructor(
    @Named(userLoginPreferenceDataStoreName) private val dataStore: DataStore<Preferences>,
) : UserLoginPreferenceGateway {
    private val loggedInUserHandlesKey = stringSetPreferencesKey("loggedInUserHandlesKey")

    override suspend fun addLoggedInUserHandle(userHandle: Long) {
        dataStore.edit { preferences ->
            val currentSet = preferences[loggedInUserHandlesKey].orEmpty()
            preferences[loggedInUserHandlesKey] = currentSet + userHandle.toString()
        }
    }

    override suspend fun hasUserLoggedInBefore(userHandle: Long): Boolean {
        val loggedInHandles = dataStore.monitor(loggedInUserHandlesKey).firstOrNull().orEmpty()
        return loggedInHandles.contains(userHandle.toString())
    }
} 
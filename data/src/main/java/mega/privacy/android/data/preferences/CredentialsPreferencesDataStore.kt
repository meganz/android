package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.gateway.preferences.CredentialsPreferencesGateway
import mega.privacy.android.domain.entity.user.UserCredentials
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal const val credentialDataStoreName = "credential"

internal class CredentialsPreferencesDataStore @Inject constructor(
    @Named(credentialDataStoreName) private val dataStore: DataStore<Preferences>,
) : CredentialsPreferencesGateway {

    override suspend fun save(credentials: UserCredentials) {
        dataStore.edit {
            it[emailKey] = credentials.email.orEmpty()
            it[sessionKey] = credentials.session.orEmpty()
            it[firstNameKey] = credentials.firstName.orEmpty()
            it[lastNameKey] = credentials.lastName.orEmpty()
            it[myHandleKey] = credentials.myHandle.orEmpty()
        }
    }

    override suspend fun saveSession(session: String) {
        dataStore.edit {
            it[sessionKey] = session
        }
    }

    override suspend fun saveMyHandle(myHandle: String) {
        dataStore.edit {
            it[myHandleKey] = myHandle
        }
    }

    override suspend fun saveFirstName(firstName: String) {
        dataStore.edit {
            it[firstNameKey] = firstName
        }
    }

    override suspend fun saveLastName(lastName: String) {
        dataStore.edit {
            it[lastNameKey] = lastName
        }
    }

    override suspend fun saveEmail(email: String) {
        dataStore.edit {
            it[emailKey] = email
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.clear()
        }
    }

    override fun monitorCredentials(): Flow<UserCredentials?> = dataStore.data.map {
        val session = it[sessionKey]
        if (session.isNullOrBlank()) return@map null
        if (session.length < MIN_USABLE_SESSION_LENGTH) {
            Timber.w("Discarding a stored session of ${session.length} chars, too short to be usable")
            return@map null
        }
        return@map UserCredentials(
            email = it[emailKey],
            session = session,
            firstName = it[firstNameKey],
            lastName = it[lastNameKey],
            myHandle = it[myHandleKey]
        )
    }

    companion object {
        /**
         * Shortest session the chat SDK can derive its cache path from. A shorter one makes
         * `MegaChatApi.init` abort the process natively, which no Kotlin caller can catch, so
         * such a session is reported as no credentials at all and the user is sent to login.
         */
        private const val MIN_USABLE_SESSION_LENGTH = 50

        private val emailKey = stringPreferencesKey("email")
        private val sessionKey = stringPreferencesKey("session")
        private val firstNameKey = stringPreferencesKey("firstName")
        private val lastNameKey = stringPreferencesKey("lastName")
        private val myHandleKey = stringPreferencesKey("myHandle")

        fun migrate(preferences: MutablePreferences, credential: UserCredentials) =
            preferences.apply {
                this[emailKey] = credential.email.orEmpty()
                this[sessionKey] = credential.session.orEmpty()
                this[firstNameKey] = credential.firstName.orEmpty()
                this[lastNameKey] = credential.lastName.orEmpty()
                this[myHandleKey] = credential.myHandle.orEmpty()
            }
    }
}
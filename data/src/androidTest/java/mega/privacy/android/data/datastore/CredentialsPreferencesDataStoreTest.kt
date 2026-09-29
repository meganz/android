package mega.privacy.android.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.FileStreamingAeadFactory
import mega.privacy.android.data.preferences.CredentialsPreferencesDataStore
import mega.privacy.android.data.preferences.base.createEncrypted
import mega.privacy.android.data.preferences.credentialDataStoreName
import mega.privacy.android.domain.entity.user.UserCredentials
import org.junit.After
import org.junit.Test

class CredentialsPreferencesDataStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file = context.preferencesDataStoreFile(credentialDataStoreName)

    @Test
    fun test_that_data_store_set_value_correctly() = runTest {
        val credentialsPreferencesGateway = CredentialsPreferencesDataStore(createDataStore(this))
        val userCredentials = UserCredentials(
            email = "email",
            session = USABLE_SESSION,
            firstName = "firstName",
            lastName = "lastName",
            myHandle = "myHandle"
        )
        credentialsPreferencesGateway.save(userCredentials)
        val credentials = credentialsPreferencesGateway.monitorCredentials().first()
        assertThat(credentials).isEqualTo(userCredentials)
    }

    @Test
    fun test_that_data_store_clears_correctly() = runTest {
        val credentialsPreferencesGateway = CredentialsPreferencesDataStore(createDataStore(this))
        val userCredentials = UserCredentials(
            email = "email",
            session = USABLE_SESSION,
            firstName = "firstName",
            lastName = "lastName",
            myHandle = "myHandle"
        )
        credentialsPreferencesGateway.save(userCredentials)
        credentialsPreferencesGateway.clear()
        val credentials = credentialsPreferencesGateway.monitorCredentials().first()
        assertThat(credentials).isNull()
    }

    @Test
    fun test_that_data_store_saves_first_name_correctly() = runTest {
        val credentialsPreferencesGateway = CredentialsPreferencesDataStore(createDataStore(this))
        val userCredentials = UserCredentials(
            email = "email",
            session = USABLE_SESSION,
            firstName = "firstName",
            lastName = "lastName",
            myHandle = "myHandle"
        )
        credentialsPreferencesGateway.save(userCredentials)
        assertThat(credentialsPreferencesGateway.monitorCredentials().first()?.firstName)
            .isEqualTo("firstName")
        val newFirstName = "newFirstName"
        credentialsPreferencesGateway.saveFirstName(newFirstName)
        assertThat(credentialsPreferencesGateway.monitorCredentials().first()?.firstName)
            .isEqualTo(newFirstName)
    }

    @Test
    fun test_that_data_store_saves_last_name_correctly() = runTest {
        val credentialsPreferencesGateway = CredentialsPreferencesDataStore(createDataStore(this))
        val userCredentials = UserCredentials(
            email = "email",
            session = USABLE_SESSION,
            firstName = "firstName",
            lastName = "lastName",
            myHandle = "myHandle"
        )
        credentialsPreferencesGateway.save(userCredentials)
        assertThat(credentialsPreferencesGateway.monitorCredentials().first()?.lastName)
            .isEqualTo("lastName")
        val newLastName = "newLastName"
        credentialsPreferencesGateway.saveLastName(newLastName)
        assertThat(credentialsPreferencesGateway.monitorCredentials().first()?.lastName)
            .isEqualTo(newLastName)
    }

    @Test
    fun test_that_data_store_saves_session_correctly() = runTest {
        val credentialsPreferencesGateway = CredentialsPreferencesDataStore(createDataStore(this))
        val userCredentials = UserCredentials(
            email = "email",
            session = USABLE_SESSION,
            firstName = "firstName",
            lastName = "lastName",
            myHandle = "myHandle"
        )
        credentialsPreferencesGateway.save(userCredentials)
        assertThat(credentialsPreferencesGateway.monitorCredentials().first()?.session)
            .isEqualTo(USABLE_SESSION)
        val newSession = NEW_USABLE_SESSION
        credentialsPreferencesGateway.saveSession(newSession)
        assertThat(credentialsPreferencesGateway.monitorCredentials().first()?.session)
            .isEqualTo(newSession)
    }

    @Test
    fun test_that_data_store_saves_my_handle_correctly() = runTest {
        val credentialsPreferencesGateway = CredentialsPreferencesDataStore(createDataStore(this))
        val userCredentials = UserCredentials(
            email = "email",
            session = USABLE_SESSION,
            firstName = "firstName",
            lastName = "lastName",
            myHandle = "oldHandle"
        )
        credentialsPreferencesGateway.save(userCredentials)
        assertThat(credentialsPreferencesGateway.monitorCredentials().first()?.myHandle)
            .isEqualTo("oldHandle")
        val newMyHandle = "newHandle"
        credentialsPreferencesGateway.saveMyHandle(newMyHandle)
        assertThat(credentialsPreferencesGateway.monitorCredentials().first()?.myHandle)
            .isEqualTo(newMyHandle)
    }

    @Test
    fun test_that_data_store_reports_no_credentials_when_the_session_is_too_short() = runTest {
        val credentialsPreferencesGateway = CredentialsPreferencesDataStore(createDataStore(this))
        val userCredentials = UserCredentials(
            email = "email",
            session = "fake-session",
            firstName = "firstName",
            lastName = "lastName",
            myHandle = "myHandle"
        )
        credentialsPreferencesGateway.save(userCredentials)
        val credentials = credentialsPreferencesGateway.monitorCredentials().first()
        assertThat(credentials).isNull()
    }

    @After
    fun tearDown() {
        file.delete()
    }

    private fun createDataStore(scope: CoroutineScope): DataStore<Preferences> {
        val streamingAead = FileStreamingAeadFactory(context)()
        return PreferenceDataStoreFactory.createEncrypted(
            scope = scope,
            context = context,
            fileName = credentialDataStoreName,
            streamingAead = streamingAead,
            crashReporter = RecordingCrashReporter(),
        )
    }

    private companion object {
        /**
         * Sessions shorter than the chat SDK can derive its cache path from are discarded by the
         * store, so these mirror the length of a real one.
         */
        const val USABLE_SESSION =
            "AWJvT_pS9tYQKhFqYw3nRxUeLdMzXcVbNmAsDfGhJkLpOiUyTrEwQ1234567890ab"
        const val NEW_USABLE_SESSION =
            "BXKwU_qT0uZRLiGrZx4oSyVfMeNaYdWcOnBtEgHiKlMqPjVzUsFxR0987654321cd"
    }
}

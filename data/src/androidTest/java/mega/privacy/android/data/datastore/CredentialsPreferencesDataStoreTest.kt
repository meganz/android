package mega.privacy.android.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
            session = "session",
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
            session = "session",
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
            session = "session",
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
            session = "session",
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

    @After
    fun tearDown() {
        file.delete()
    }

    private fun createDataStore(scope: CoroutineScope): DataStore<Preferences> {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return PreferenceDataStoreFactory.createEncrypted(scope = scope) {
            EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
        }
    }
}
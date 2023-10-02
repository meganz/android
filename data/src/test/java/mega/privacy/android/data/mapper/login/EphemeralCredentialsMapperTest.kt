package mega.privacy.android.data.mapper.login

import androidx.datastore.preferences.core.MutablePreferences
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.preferences.EphemeralCredentialsDataStore
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class EphemeralCredentialsMapperTest {
    private val decryptData = mock<DecryptData>()
    private val underTest = EphemeralCredentialsMapper(decryptData)

    @Test
    fun `test that mapper returns null when session is empty`() = runTest {
        val preference = mock<MutablePreferences>()
        val session = ""
        whenever(preference[EphemeralCredentialsDataStore.sessionPreferenceKey]).thenReturn(session)
        whenever(decryptData(session)).thenReturn("")
        Truth.assertThat(underTest(preference)).isNull()
    }

    @Test
    fun `test that mapper returns model when session is not empty`() = runTest {
        val preference = mock<MutablePreferences>()
        val encryptedSession = "encryptedSession"
        val encryptedPassword = "encryptedPassword"
        val encryptedFirstName = "encryptedFirstName"
        val encryptedLastName = "encryptedLastName"
        val encryptedEmail = "encryptedEmail"
        val expected = EphemeralCredentials(
            email = "email",
            session = "session",
            firstName = "firstName",
            lastName = "lastName",
            password = "password"
        )
        whenever(preference[EphemeralCredentialsDataStore.sessionPreferenceKey])
            .thenReturn(encryptedSession)
        whenever(preference[EphemeralCredentialsDataStore.emailPreferenceKey])
            .thenReturn(encryptedEmail)
        whenever(preference[EphemeralCredentialsDataStore.lastNamePreferenceKey])
            .thenReturn(encryptedLastName)
        whenever(preference[EphemeralCredentialsDataStore.firstNamePreferenceKey])
            .thenReturn(encryptedFirstName)
        whenever(preference[EphemeralCredentialsDataStore.passwordPreferenceKey])
            .thenReturn(encryptedPassword)
        whenever(decryptData(encryptedSession)).thenReturn(expected.session)
        whenever(decryptData(encryptedPassword)).thenReturn(expected.password)
        whenever(decryptData(encryptedFirstName)).thenReturn(expected.firstName)
        whenever(decryptData(encryptedLastName)).thenReturn(expected.lastName)
        whenever(decryptData(encryptedEmail)).thenReturn(expected.email)
        Truth.assertThat(underTest(preference)).isEqualTo(expected)
    }
}
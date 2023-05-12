package mega.privacy.android.data.mapper.login

import androidx.datastore.preferences.core.MutablePreferences
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.preferences.EphemeralCredentialsDataStore
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class EphemeralCredentialsPreferenceMapperTest {
    private val encryptData: EncryptData = mock()
    private lateinit var underTest: EphemeralCredentialsPreferenceMapper

    @Before
    fun setUp() {
        underTest = EphemeralCredentialsPreferenceMapper(encryptData)
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val ephemeralCredentials = mock<EphemeralCredentials> {
            on { session }.thenReturn("session")
            on { email }.thenReturn("email")
            on { lastName }.thenReturn("lastName")
            on { firstName }.thenReturn("firstName")
            on { password }.thenReturn("password")
        }
        whenever(encryptData(ephemeralCredentials.session)).thenReturn("encryptedSession")
        whenever(encryptData(ephemeralCredentials.email)).thenReturn("encryptedEmail")
        whenever(encryptData(ephemeralCredentials.lastName)).thenReturn("encryptedLastName")
        whenever(encryptData(ephemeralCredentials.firstName)).thenReturn("encryptedFirstName")
        whenever(encryptData(ephemeralCredentials.password)).thenReturn("encryptedPassword")
        val preference = mock<MutablePreferences>()
        underTest(preference, ephemeralCredentials)
        verify(preference)[EphemeralCredentialsDataStore.sessionPreferenceKey] = "encryptedSession"
        verify(preference)[EphemeralCredentialsDataStore.emailPreferenceKey] = "encryptedEmail"
        verify(preference)[EphemeralCredentialsDataStore.lastNamePreferenceKey] = "encryptedLastName"
        verify(preference)[EphemeralCredentialsDataStore.firstNamePreferenceKey] = "encryptedFirstName"
        verify(preference)[EphemeralCredentialsDataStore.passwordPreferenceKey] = "encryptedPassword"
    }
}
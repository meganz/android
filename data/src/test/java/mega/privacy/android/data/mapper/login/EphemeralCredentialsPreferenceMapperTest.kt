package mega.privacy.android.data.mapper.login

import androidx.datastore.preferences.core.MutablePreferences
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.preferences.EphemeralCredentialsDataStore
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [EphemeralCredentialsMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EphemeralCredentialsPreferenceMapperTest {
    private lateinit var underTest: EphemeralCredentialsPreferenceMapper

    private val encryptData: EncryptData = mock()

    @BeforeAll
    fun setUp() {
        underTest = EphemeralCredentialsPreferenceMapper(encryptData)
    }

    @BeforeEach
    fun resetMocks() {
        reset(encryptData)
    }

    @Test
    fun `test that the mapper returns the model`() = runTest {
        val ephemeralCredentials = mock<EphemeralCredentials> {
            on { session }.thenReturn("session")
            on { email }.thenReturn("email")
            on { lastName }.thenReturn("lastName")
            on { firstName }.thenReturn("firstName")
            on { password }.thenReturn("password")
        }
        val preference = mock<MutablePreferences>()

        whenever(encryptData(ephemeralCredentials.session)).thenReturn("encryptedSession")
        whenever(encryptData(ephemeralCredentials.email)).thenReturn("encryptedEmail")
        whenever(encryptData(ephemeralCredentials.lastName)).thenReturn("encryptedLastName")
        whenever(encryptData(ephemeralCredentials.firstName)).thenReturn("encryptedFirstName")
        whenever(encryptData(ephemeralCredentials.password)).thenReturn("encryptedPassword")

        underTest(preference, ephemeralCredentials)

        verify(preference)[EphemeralCredentialsDataStore.sessionPreferenceKey] = "encryptedSession"
        verify(preference)[EphemeralCredentialsDataStore.emailPreferenceKey] = "encryptedEmail"
        verify(preference)[EphemeralCredentialsDataStore.lastNamePreferenceKey] =
            "encryptedLastName"
        verify(preference)[EphemeralCredentialsDataStore.firstNamePreferenceKey] =
            "encryptedFirstName"
        verify(preference)[EphemeralCredentialsDataStore.passwordPreferenceKey] =
            "encryptedPassword"
    }
}
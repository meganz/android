package mega.privacy.android.data.mapper.login

import androidx.datastore.preferences.core.MutablePreferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.preferences.EphemeralCredentialsDataStore
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [EphemeralCredentialsMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EphemeralCredentialsMapperTest {
    private lateinit var underTest: EphemeralCredentialsMapper

    private val decryptData = mock<DecryptData>()

    @BeforeAll
    fun setUp() {
        underTest = EphemeralCredentialsMapper(decryptData)
    }

    @BeforeEach
    fun resetMocks() {
        reset(decryptData)
    }

    @Test
    fun `test that the mapper returns null when the session is empty`() = runTest {
        val preference = mock<MutablePreferences>()
        val session = ""

        whenever(preference[EphemeralCredentialsDataStore.sessionPreferenceKey]).thenReturn(session)
        whenever(decryptData(session)).thenReturn("")

        assertThat(underTest(preference)).isNull()
    }

    @Test
    fun `test that the mapper returns null when the session only contains blanks`() = runTest {
        val preference = mock<MutablePreferences>()
        val session = " "

        whenever(preference[EphemeralCredentialsDataStore.sessionPreferenceKey]).thenReturn(" ")
        whenever(decryptData(session)).thenReturn(" ")

        assertThat(underTest(preference)).isNull()
    }

    @Test
    fun `test that the mapper returns the model when the session is not empty`() = runTest {
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

        assertThat(underTest(preference)).isEqualTo(expected)
    }
}
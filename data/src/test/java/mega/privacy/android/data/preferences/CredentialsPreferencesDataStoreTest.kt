package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test class for [CredentialsPreferencesDataStore]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CredentialsPreferencesDataStoreTest {
    private lateinit var underTest: CredentialsPreferencesDataStore

    private val emailKey = stringPreferencesKey("email")
    private val sessionKey = stringPreferencesKey("session")
    private val firstNameKey = stringPreferencesKey("firstName")
    private val lastNameKey = stringPreferencesKey("lastName")
    private val myHandleKey = stringPreferencesKey("myHandle")

    private val preferences = mock<Preferences>()

    private val dataStore = mock<DataStore<Preferences>> {
        on { data }.thenReturn(flow {
            emit(preferences)
            awaitCancellation()
        })
    }

    @BeforeEach
    internal fun setUp() {
        underTest = CredentialsPreferencesDataStore(dataStore)
        whenever(preferences[emailKey]).thenReturn("test@mega.nz")
        whenever(preferences[firstNameKey]).thenReturn("First")
        whenever(preferences[lastNameKey]).thenReturn("Last")
        whenever(preferences[myHandleKey]).thenReturn("111")
    }

    @Test
    internal fun `test that monitorCredentials emits null when no session is stored`() = runTest {
        whenever(preferences[sessionKey]).thenReturn(null)

        assertThat(underTest.monitorCredentials().first()).isNull()
    }

    @Test
    internal fun `test that monitorCredentials emits null when the session is blank`() = runTest {
        whenever(preferences[sessionKey]).thenReturn("   ")

        assertThat(underTest.monitorCredentials().first()).isNull()
    }

    @Test
    internal fun `test that monitorCredentials emits null when the session is too short to be usable`() =
        runTest {
            whenever(preferences[sessionKey]).thenReturn("fake-session")

            assertThat(underTest.monitorCredentials().first()).isNull()
        }

    @Test
    internal fun `test that monitorCredentials emits the credentials when a usable session is stored`() =
        runTest {
            whenever(preferences[sessionKey]).thenReturn(USABLE_SESSION)

            val credentials = underTest.monitorCredentials().first()

            assertThat(credentials?.session).isEqualTo(USABLE_SESSION)
            assertThat(credentials?.email).isEqualTo("test@mega.nz")
            assertThat(credentials?.firstName).isEqualTo("First")
            assertThat(credentials?.lastName).isEqualTo("Last")
            assertThat(credentials?.myHandle).isEqualTo("111")
        }

    private companion object {
        const val USABLE_SESSION =
            "AWJvT_pS9tYQKhFqYw3nRxUeLdMzXcVbNmAsDfGhJkLpOiUyTrEwQ1234567890ab"
    }
}

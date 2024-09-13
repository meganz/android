package mega.privacy.android.data.preferences.migration

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.domain.entity.user.UserCredentials
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CredentialsPreferencesMigrationTest {
    private lateinit var underTest: CredentialsPreferencesMigration
    private val databaseHandler: DatabaseHandler = mock()

    @BeforeEach
    internal fun setUp() {
        underTest = CredentialsPreferencesMigration { databaseHandler }
    }

    @Test
    fun `test that should migrate returns true if no keys exist`() = runTest {
        val currentData = mock<Preferences> { on { asMap() }.thenReturn(emptyMap()) }
        val actual = underTest.shouldMigrate(currentData)
        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `test that data store set value correctly`() = runTest {
        val credential = mock<UserCredentials> {
            on { email }.thenReturn("email")
            on { session }.thenReturn("session")
            on { firstName }.thenReturn("firstName")
            on { lastName }.thenReturn("lastName")
            on { myHandle }.thenReturn("myHandle")
        }
        whenever(databaseHandler.credentials).thenReturn(credential)
        val mutablePreferences = mock<MutablePreferences>()
        val preference = mock<Preferences> {
            on { toMutablePreferences() }.thenReturn(mutablePreferences)
        }
        underTest.migrate(preference)
        verify(mutablePreferences)[stringPreferencesKey("email")] = "email"
        verify(mutablePreferences)[stringPreferencesKey("session")] = "session"
        verify(mutablePreferences)[stringPreferencesKey("firstName")] = "firstName"
        verify(mutablePreferences)[stringPreferencesKey("lastName")] = "lastName"
        verify(mutablePreferences)[stringPreferencesKey("myHandle")] = "myHandle"
    }
}
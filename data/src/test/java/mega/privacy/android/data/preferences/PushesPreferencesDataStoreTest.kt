package mega.privacy.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PushesPreferencesDataStoreTest {

    private lateinit var underTest: PushesPreferencesDataStore

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher()),
            produceFile = {
                context.preferencesDataStoreFile(PushesPreferencesDataStore.PUSH_TOKEN_FILE)
            }
        )

    @Before
    fun setup() {
        underTest = PushesPreferencesDataStore(dataStore = dataStore)
    }

    @Test
    fun `test that getPushToken returns an empty string when no token has been set`() = runTest {
        underTest.clearPushToken()

        assertThat(underTest.getPushToken()).isEmpty()
    }

    @Test
    fun `test that getPushToken returns the token set by setPushToken`() = runTest {
        val expected = "new-push-token"

        underTest.setPushToken(expected)

        assertThat(underTest.getPushToken()).isEqualTo(expected)
    }

    @Test
    fun `test that setPushToken overwrites a previously stored token`() = runTest {
        val expected = "second-token"

        underTest.setPushToken("first-token")
        underTest.setPushToken(expected)

        assertThat(underTest.getPushToken()).isEqualTo(expected)
    }

    @Test
    fun `test that clearPushToken removes the stored token`() = runTest {
        underTest.setPushToken("a-token")

        underTest.clearPushToken()

        assertThat(underTest.getPushToken()).isEmpty()
    }
}

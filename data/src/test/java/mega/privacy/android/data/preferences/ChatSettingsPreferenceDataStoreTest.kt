package mega.privacy.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.settings.ChatSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test class for [ChatSettingsPreferenceDataStore]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatSettingsPreferenceDataStoreTest {
    private lateinit var underTest: ChatSettingsPreferenceDataStore

    private val notificationsSoundKey = stringPreferencesKey("notificationsSound")
    private val vibrationEnabledKey = stringPreferencesKey("vibrationEnabled")
    private val videoQualityKey = stringPreferencesKey("videoQuality")

    private val preferences = mock<Preferences>()

    private val dataStore = mock<DataStore<Preferences>> {
        on { data }.thenReturn(flow {
            emit(preferences)
            awaitCancellation()
        })
    }

    @BeforeAll
    internal fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = ChatSettingsPreferenceDataStore(dataStore)
    }

    @AfterAll
    internal fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    internal fun `test that null is returned when no chat settings are stored`() = runTest {
        whenever(preferences[notificationsSoundKey]).thenReturn(null)
        whenever(preferences[vibrationEnabledKey]).thenReturn(null)
        whenever(preferences[videoQualityKey]).thenReturn(null)

        assertThat(underTest.getChatSettings()).isNull()
    }

    @Test
    internal fun `test that the stored chat settings are returned`() = runTest {
        whenever(preferences[notificationsSoundKey]).thenReturn("sound")
        whenever(preferences[vibrationEnabledKey]).thenReturn(ChatSettings.VIBRATION_OFF)
        whenever(preferences[videoQualityKey]).thenReturn("2")

        assertThat(underTest.getChatSettings()).isEqualTo(
            ChatSettings(
                notificationsSound = "sound",
                vibrationEnabled = ChatSettings.VIBRATION_OFF,
                videoQuality = "2",
            )
        )
    }

    @Test
    internal fun `test that defaults are used for missing values when at least one value is present`() =
        runTest {
            whenever(preferences[notificationsSoundKey]).thenReturn("sound")
            whenever(preferences[vibrationEnabledKey]).thenReturn(null)
            whenever(preferences[videoQualityKey]).thenReturn(null)

            val defaults = ChatSettings()
            assertThat(underTest.getChatSettings()).isEqualTo(
                ChatSettings(
                    notificationsSound = "sound",
                    vibrationEnabled = defaults.vibrationEnabled,
                    videoQuality = defaults.videoQuality,
                )
            )
        }
}

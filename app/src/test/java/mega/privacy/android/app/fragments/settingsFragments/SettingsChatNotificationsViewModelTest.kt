package mega.privacy.android.app.fragments.settingsFragments

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_OFF
import mega.privacy.android.domain.entity.settings.ChatSettings.Companion.VIBRATION_ON
import mega.privacy.android.domain.usecase.setting.GetChatSettingsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorChatSettingsUseCase
import mega.privacy.android.domain.usecase.setting.SetChatSettingsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsChatNotificationsViewModelTest {
    private lateinit var underTest: SettingsChatNotificationsViewModel

    private val monitorChatSettingsUseCase = mock<MonitorChatSettingsUseCase>()
    private val getChatSettingsUseCase = mock<GetChatSettingsUseCase>()
    private val setChatSettingsUseCase = mock<SetChatSettingsUseCase>()

    private fun initUnderTest() {
        underTest = SettingsChatNotificationsViewModel(
            monitorChatSettingsUseCase = monitorChatSettingsUseCase,
            getChatSettingsUseCase = getChatSettingsUseCase,
            setChatSettingsUseCase = setChatSettingsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(monitorChatSettingsUseCase, getChatSettingsUseCase, setChatSettingsUseCase)
        whenever(monitorChatSettingsUseCase()).thenReturn(emptyFlow())
    }

    @Test
    fun `test that initial uiState has vibration enabled and no sound`() = runTest {
        initUnderTest()

        val state = underTest.uiState.value
        assertThat(state.isVibrationEnabled).isTrue()
        assertThat(state.notificationsSound).isNull()
    }

    @Test
    fun `test that uiState reflects the monitored chat settings`() = runTest {
        whenever(monitorChatSettingsUseCase()).thenReturn(
            flowOf(
                ChatSettings(
                    notificationsSound = "sound",
                    vibrationEnabled = VIBRATION_OFF,
                )
            )
        )
        initUnderTest()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.notificationsSound).isEqualTo("sound")
            assertThat(state.isVibrationEnabled).isFalse()
        }
    }

    @Test
    fun `test that uiState has vibration enabled when chat settings are null`() = runTest {
        whenever(monitorChatSettingsUseCase()).thenReturn(flowOf(null))
        initUnderTest()

        underTest.uiState.test {
            assertThat(awaitItem().isVibrationEnabled).isTrue()
        }
    }

    @Test
    fun `test that toggleVibration disables vibration when currently enabled`() = runTest {
        whenever(getChatSettingsUseCase()).thenReturn(ChatSettings(vibrationEnabled = VIBRATION_ON))
        initUnderTest()

        underTest.toggleVibration()

        verify(setChatSettingsUseCase).invoke(ChatSettings(vibrationEnabled = VIBRATION_OFF))
    }

    @Test
    fun `test that toggleVibration enables vibration when currently disabled`() = runTest {
        whenever(getChatSettingsUseCase()).thenReturn(ChatSettings(vibrationEnabled = VIBRATION_OFF))
        initUnderTest()

        underTest.toggleVibration()

        verify(setChatSettingsUseCase).invoke(ChatSettings(vibrationEnabled = VIBRATION_ON))
    }

    @Test
    fun `test that toggleVibration uses default settings when none are stored`() = runTest {
        whenever(getChatSettingsUseCase()).thenReturn(null)
        initUnderTest()

        underTest.toggleVibration()

        verify(setChatSettingsUseCase).invoke(ChatSettings(vibrationEnabled = VIBRATION_OFF))
    }

    @Test
    fun `test that setNotificationSound stores the sound preserving other fields`() = runTest {
        whenever(getChatSettingsUseCase()).thenReturn(ChatSettings(vibrationEnabled = VIBRATION_OFF))
        initUnderTest()

        underTest.setNotificationSound("new-sound")

        verify(setChatSettingsUseCase).invoke(
            ChatSettings(notificationsSound = "new-sound", vibrationEnabled = VIBRATION_OFF)
        )
    }
}

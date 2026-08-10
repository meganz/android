package mega.privacy.android.app.presentation.settings.chat

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.chat.link.MonitorRichLinkPreviewConfigUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
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
class SettingsChatViewModelTest {
    private lateinit var underTest: SettingsChatViewModel

    private val getChatImageQuality = mock<GetChatImageQuality>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val monitorRichLinkPreviewConfigUseCase = mock<MonitorRichLinkPreviewConfigUseCase>()
    private val monitorChatSettingsUseCase = mock<MonitorChatSettingsUseCase>()
    private val getChatSettingsUseCase = mock<GetChatSettingsUseCase>()
    private val setChatSettingsUseCase = mock<SetChatSettingsUseCase>()

    private fun initUnderTest() {
        underTest = SettingsChatViewModel(
            getChatImageQuality = getChatImageQuality,
            ioDispatcher = UnconfinedTestDispatcher(),
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            monitorRichLinkPreviewConfigUseCase = monitorRichLinkPreviewConfigUseCase,
            monitorChatSettingsUseCase = monitorChatSettingsUseCase,
            getChatSettingsUseCase = getChatSettingsUseCase,
            setChatSettingsUseCase = setChatSettingsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getChatImageQuality,
            monitorConnectivityUseCase,
            isConnectedToInternetUseCase,
            monitorRichLinkPreviewConfigUseCase,
            monitorChatSettingsUseCase,
            getChatSettingsUseCase,
            setChatSettingsUseCase,
        )
        whenever(getChatImageQuality()).thenReturn(emptyFlow())
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(monitorRichLinkPreviewConfigUseCase()).thenReturn(emptyFlow())
        whenever(monitorChatSettingsUseCase()).thenReturn(emptyFlow())
    }

    @Test
    fun `test that state chatVideoQuality reflects the monitored chat settings`() = runTest {
        whenever(monitorChatSettingsUseCase()).thenReturn(
            flowOf(ChatSettings(videoQuality = VideoQuality.HIGH.value.toString()))
        )
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().chatVideoQuality).isEqualTo(VideoQuality.HIGH.value)
        }
    }

    @Test
    fun `test that state chatVideoQuality defaults to medium when chat settings are null`() =
        runTest {
            whenever(monitorChatSettingsUseCase()).thenReturn(flowOf(null))
            initUnderTest()

            underTest.state.test {
                assertThat(awaitItem().chatVideoQuality).isEqualTo(VideoQuality.MEDIUM.value)
            }
        }

    @Test
    fun `test that setChatVideoQuality persists the new quality on the current chat settings`() =
        runTest {
            val current = ChatSettings(
                notificationsSound = "sound",
                videoQuality = VideoQuality.MEDIUM.value.toString(),
            )
            whenever(getChatSettingsUseCase()).thenReturn(current)
            initUnderTest()

            underTest.setChatVideoQuality(VideoQuality.HIGH.value)

            verify(setChatSettingsUseCase).invoke(
                current.copy(videoQuality = VideoQuality.HIGH.value.toString())
            )
        }

    @Test
    fun `test that setChatVideoQuality uses default chat settings when none exist`() = runTest {
        whenever(getChatSettingsUseCase()).thenReturn(null)
        initUnderTest()

        underTest.setChatVideoQuality(VideoQuality.ORIGINAL.value)

        verify(setChatSettingsUseCase).invoke(
            ChatSettings(videoQuality = VideoQuality.ORIGINAL.value.toString())
        )
    }
}

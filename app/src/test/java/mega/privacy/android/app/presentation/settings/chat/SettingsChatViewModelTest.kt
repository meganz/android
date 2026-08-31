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
import mega.privacy.android.domain.entity.chat.ChatPresenceConfig
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.MonitorChatSignalPresenceUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.SignalChatPresenceActivity
import mega.privacy.android.domain.usecase.chat.GetChatPresenceConfigUseCase
import mega.privacy.android.domain.usecase.chat.SetCurrentUserStatusUseCase
import mega.privacy.android.domain.usecase.chat.SetPresenceAutoAwayUseCase
import mega.privacy.android.domain.usecase.chat.SetPresencePersistUseCase
import mega.privacy.android.domain.usecase.chat.link.EnableRichPreviewUseCase
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
    private val monitorChatSignalPresenceUseCase = mock<MonitorChatSignalPresenceUseCase>()
    private val getChatPresenceConfigUseCase = mock<GetChatPresenceConfigUseCase>()
    private val setCurrentUserStatusUseCase = mock<SetCurrentUserStatusUseCase>()
    private val setPresenceAutoAwayUseCase = mock<SetPresenceAutoAwayUseCase>()
    private val setPresencePersistUseCase = mock<SetPresencePersistUseCase>()
    private val signalChatPresenceActivity = mock<SignalChatPresenceActivity>()
    private val enableRichPreviewUseCase = mock<EnableRichPreviewUseCase>()
    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()

    private val presenceConfig = ChatPresenceConfig(
        onlineStatus = UserChatStatus.Online,
        isAutoAwayEnabled = true,
        autoAwayTimeout = 300L,
        isPersist = false,
        isPending = false,
        isLastGreenVisible = true,
    )

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
            monitorChatSignalPresenceUseCase = monitorChatSignalPresenceUseCase,
            getChatPresenceConfigUseCase = getChatPresenceConfigUseCase,
            setCurrentUserStatusUseCase = setCurrentUserStatusUseCase,
            setPresenceAutoAwayUseCase = setPresenceAutoAwayUseCase,
            setPresencePersistUseCase = setPresencePersistUseCase,
            signalChatPresenceActivity = signalChatPresenceActivity,
            enableRichPreviewUseCase = enableRichPreviewUseCase,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
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
            monitorChatSignalPresenceUseCase,
            getChatPresenceConfigUseCase,
            setCurrentUserStatusUseCase,
            setPresenceAutoAwayUseCase,
            setPresencePersistUseCase,
            signalChatPresenceActivity,
            enableRichPreviewUseCase,
            rootNodeExistsUseCase,
        )
        whenever(getChatImageQuality()).thenReturn(emptyFlow())
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())
        whenever(monitorRichLinkPreviewConfigUseCase()).thenReturn(emptyFlow())
        whenever(monitorChatSettingsUseCase()).thenReturn(emptyFlow())
        whenever(monitorChatSignalPresenceUseCase()).thenReturn(emptyFlow())
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

    @Test
    fun `test that state chatPresenceConfig reflects the loaded presence config`() = runTest {
        whenever(getChatPresenceConfigUseCase()).thenReturn(presenceConfig)
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().chatPresenceConfig).isEqualTo(presenceConfig)
        }
    }

    @Test
    fun `test that state chatPresenceConfig refreshes when chat signal presence is emitted`() =
        runTest {
            val updatedConfig = presenceConfig.copy(isPersist = true)
            whenever(monitorChatSignalPresenceUseCase()).thenReturn(flowOf(Unit))
            whenever(getChatPresenceConfigUseCase()).thenReturn(presenceConfig, updatedConfig)
            initUnderTest()

            underTest.state.test {
                assertThat(awaitItem().chatPresenceConfig).isEqualTo(updatedConfig)
            }
        }

    @Test
    fun `test that state rootNodeExists reflects the root node exists use case`() = runTest {
        whenever(rootNodeExistsUseCase()).thenReturn(true)
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().rootNodeExists).isTrue()
        }
    }

    @Test
    fun `test that signalChatPresence signals chat presence activity`() = runTest {
        initUnderTest()

        underTest.signalChatPresence()

        verify(signalChatPresenceActivity).invoke()
    }

    @ParameterizedTest(name = "value {0} maps to {1}")
    @CsvSource(
        "3, Online",
        "2, Away",
        "4, Busy",
        "1, Offline",
    )
    fun `test that setUserStatus sets the user chat status matching the preference value`(
        statusValue: Int,
        expectedStatus: UserChatStatus,
    ) = runTest {
        initUnderTest()

        underTest.setUserStatus(statusValue)

        verify(setCurrentUserStatusUseCase).invoke(expectedStatus)
    }

    @Test
    fun `test that setUserStatus does not set any user chat status when the value is unknown`() =
        runTest {
            initUnderTest()

            underTest.setUserStatus(99)

            verifyNoInteractions(setCurrentUserStatusUseCase)
        }

    @Test
    fun `test that setAutoAwayEnabled enables auto away with the default timeout`() = runTest {
        initUnderTest()

        underTest.setAutoAwayEnabled(true)

        verify(setPresenceAutoAwayUseCase).invoke(true, 300)
    }

    @Test
    fun `test that setAutoAwayEnabled disables auto away with a zero timeout`() = runTest {
        initUnderTest()

        underTest.setAutoAwayEnabled(false)

        verify(setPresenceAutoAwayUseCase).invoke(false, 0)
    }

    @Test
    fun `test that setPresencePersist invokes its use case`() = runTest {
        initUnderTest()

        underTest.setPresencePersist(true)

        verify(setPresencePersistUseCase).invoke(true)
    }

    @Test
    fun `test that enableRichLinkPreview invokes its use case`() = runTest {
        initUnderTest()

        underTest.enableRichLinkPreview(true)

        verify(enableRichPreviewUseCase).invoke(true)
    }
}

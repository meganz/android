package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatNotificationMuteState
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorChatNotificationMuteStateUseCaseTest {
    private lateinit var underTest: MonitorChatNotificationMuteStateUseCase

    private val settingsUpdateFlow = MutableSharedFlow<Boolean>()
    private val isChatNotificationMuteUseCase = mock<IsChatNotificationMuteUseCase>()
    private val getChatDoNotDisturbTimeUseCase = mock<GetChatDoNotDisturbTimeUseCase>()
    private val monitorUpdatePushNotificationSettingsUseCase =
        mock<MonitorUpdatePushNotificationSettingsUseCase>()

    private val chatId = 123L

    @BeforeAll
    fun setUp() {
        underTest = MonitorChatNotificationMuteStateUseCase(
            isChatNotificationMuteUseCase = isChatNotificationMuteUseCase,
            getChatDoNotDisturbTimeUseCase = getChatDoNotDisturbTimeUseCase,
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isChatNotificationMuteUseCase,
            getChatDoNotDisturbTimeUseCase,
            monitorUpdatePushNotificationSettingsUseCase,
        )
        whenever(monitorUpdatePushNotificationSettingsUseCase())
            .thenReturn(settingsUpdateFlow)
    }

    @Test
    fun `test that invoke emits unmuted state initially when the chat is not muted`() = runTest {
        whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(false)

        underTest(chatId).test {
            assertThat(awaitItem()).isEqualTo(
                ChatNotificationMuteState(isMuted = false, mutedUntilTimestamp = null)
            )
        }
    }

    @Test
    fun `test that invoke emits the muted until timestamp when the chat is muted with a timestamp`() =
        runTest {
            whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(true)
            whenever(getChatDoNotDisturbTimeUseCase(chatId)).thenReturn(1234567890L)

            underTest(chatId).test {
                assertThat(awaitItem()).isEqualTo(
                    ChatNotificationMuteState(isMuted = true, mutedUntilTimestamp = 1234567890L)
                )
            }
        }

    @Test
    fun `test that invoke emits a null timestamp when the chat is muted indefinitely`() = runTest {
        whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(true)
        whenever(getChatDoNotDisturbTimeUseCase(chatId)).thenReturn(0L)

        underTest(chatId).test {
            assertThat(awaitItem()).isEqualTo(
                ChatNotificationMuteState(isMuted = true, mutedUntilTimestamp = null)
            )
        }
    }

    @Test
    fun `test that invoke recomputes the mute state when push notification settings update`() =
        runTest {
            whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(false)

            underTest(chatId).test {
                assertThat(awaitItem().isMuted).isFalse()
                whenever(isChatNotificationMuteUseCase(chatId)).thenReturn(true)
                whenever(getChatDoNotDisturbTimeUseCase(chatId)).thenReturn(99L)
                settingsUpdateFlow.emit(true)
                assertThat(awaitItem()).isEqualTo(
                    ChatNotificationMuteState(isMuted = true, mutedUntilTimestamp = 99L)
                )
            }
        }
}

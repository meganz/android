package mega.privacy.android.app.presentation.settings

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.chat.SetLastGreenVisibleUseCase
import mega.privacy.android.domain.usecase.chat.SetPresenceAutoAwayUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class ChatPreferencesViewModelTest {

    private lateinit var underTest: ChatPreferencesViewModel

    //Mocks
    private val monitorUpdatePushNotificationSettingsUseCase =
        mock<MonitorUpdatePushNotificationSettingsUseCase> {
            on { invoke() }.thenReturn(flowOf(true))
        }

    private val setPresenceAutoAwayUseCase = mock<SetPresenceAutoAwayUseCase>()

    private val setLastGreenVisibleUseCase = mock<SetLastGreenVisibleUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = ChatPreferencesViewModel(
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
            setPresenceAutoAwayUseCase = setPresenceAutoAwayUseCase,
            setLastGreenVisibleUseCase = setLastGreenVisibleUseCase,
        )
    }

    @Test
    fun `test that when push notification settings is updated state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorUpdatePushNotificationSettingsUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isPushNotificationSettingsUpdatedEvent).isTrue()
            }
        }

    @Test
    fun `test that when onConsumePushNotificationSettingsUpdateEvent is called then state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorUpdatePushNotificationSettingsUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isPushNotificationSettingsUpdatedEvent).isTrue()
                underTest.onConsumePushNotificationSettingsUpdateEvent()
                val updatedState = awaitItem()
                Truth.assertThat(updatedState.isPushNotificationSettingsUpdatedEvent).isFalse()
            }
        }

    @Test
    fun `test that setAutoAwayPresence enables auto away with the given timeout`() =
        runTest {
            underTest.setAutoAwayPresence(300)
            testScheduler.advanceUntilIdle()

            verify(setPresenceAutoAwayUseCase).invoke(true, 300)
        }

    @Test
    fun `test that setLastGreenVisible invokes its use case`() =
        runTest {
            underTest.setLastGreenVisible(true)
            testScheduler.advanceUntilIdle()

            verify(setLastGreenVisibleUseCase).invoke(true)
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}

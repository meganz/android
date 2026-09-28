package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorChatsDoNotDisturbEnabledUseCaseTest {
    private lateinit var underTest: MonitorChatsDoNotDisturbEnabledUseCase

    private val settingsUpdateFlow = MutableSharedFlow<Boolean>()
    private val notificationsRepository = mock<NotificationsRepository>()
    private val monitorUpdatePushNotificationSettingsUseCase =
        mock<MonitorUpdatePushNotificationSettingsUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorChatsDoNotDisturbEnabledUseCase(
            notificationsRepository = notificationsRepository,
            monitorUpdatePushNotificationSettingsUseCase = monitorUpdatePushNotificationSettingsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(notificationsRepository, monitorUpdatePushNotificationSettingsUseCase)
        whenever(monitorUpdatePushNotificationSettingsUseCase()).thenReturn(settingsUpdateFlow)
    }

    @Test
    fun `test that invoke emits the current state first`() = runTest {
        whenever(notificationsRepository.isChatsDoNotDisturbEnabled()).thenReturn(true)

        underTest().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that invoke recomputes the state when push notification settings update`() =
        runTest {
            whenever(notificationsRepository.isChatsDoNotDisturbEnabled()).thenReturn(false)

            underTest().test {
                assertThat(awaitItem()).isFalse()
                whenever(notificationsRepository.isChatsDoNotDisturbEnabled()).thenReturn(true)
                settingsUpdateFlow.emit(true)
                assertThat(awaitItem()).isTrue()
            }
        }
}

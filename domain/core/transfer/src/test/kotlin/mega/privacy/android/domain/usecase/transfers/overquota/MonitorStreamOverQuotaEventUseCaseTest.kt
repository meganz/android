package mega.privacy.android.domain.usecase.transfers.overquota

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.StreamOverQuotaEvent
import mega.privacy.android.domain.entity.TransferQuotaStateEvent
import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorStreamOverQuotaEventUseCaseTest {

    private val notificationsRepository = mock<NotificationsRepository>()

    private val underTest = MonitorStreamOverQuotaEventUseCase(
        notificationsRepository = notificationsRepository,
    )

    @BeforeEach
    fun cleanUp() {
        reset(notificationsRepository)
        notificationsRepository.stub {
            on { monitorEvent() } doReturn emptyFlow()
            on { monitorFolderLinkEvent() } doReturn emptyFlow()
        }
    }

    @Test
    fun `test that the time left raised as a global event is emitted`() = runTest {
        notificationsRepository.stub {
            on { monitorEvent() } doReturn flowOf(
                StreamOverQuotaEvent(handle = 1L, timeLeft = 10.seconds)
            )
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(10.seconds)
            awaitComplete()
        }
    }

    @Test
    fun `test that the time left raised by the folder link instance is emitted`() = runTest {
        notificationsRepository.stub {
            on { monitorFolderLinkEvent() } doReturn flowOf(
                StreamOverQuotaEvent(handle = 1L, timeLeft = 10.seconds)
            )
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(10.seconds)
            awaitComplete()
        }
    }

    @Test
    fun `test that an event without time left is not emitted`() = runTest {
        notificationsRepository.stub {
            on { monitorEvent() } doReturn flowOf(
                StreamOverQuotaEvent(handle = 1L, timeLeft = Duration.ZERO)
            )
        }

        underTest().test {
            awaitComplete()
        }
    }

    @Test
    fun `test that other global events are not emitted`() = runTest {
        notificationsRepository.stub {
            on { monitorEvent() } doReturn flowOf(
                TransferQuotaStateEvent(handle = 1L, state = TransferQuotaState.Orange)
            )
        }

        underTest().test {
            awaitComplete()
        }
    }
}

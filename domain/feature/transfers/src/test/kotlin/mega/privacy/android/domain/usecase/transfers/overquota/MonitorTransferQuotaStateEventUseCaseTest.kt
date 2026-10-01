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
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorTransferQuotaStateEventUseCaseTest {

    private val notificationsRepository = mock<NotificationsRepository>()
    private val transferRepository = mock<TransferRepository>()

    private val underTest = MonitorTransferQuotaStateEventUseCase(
        notificationsRepository = notificationsRepository,
        transferRepository = transferRepository,
    )

    @BeforeEach
    fun cleanUp() {
        reset(notificationsRepository, transferRepository)
    }

    @Test
    fun `test that the state raised as a global event is emitted`() = runTest {
        notificationsRepository.stub {
            on { monitorEvent() } doReturn flowOf(
                TransferQuotaStateEvent(handle = 1L, state = TransferQuotaState.Orange)
            )
        }
        transferRepository.stub { on { monitorTransferQuotaState() } doReturn emptyFlow() }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(TransferQuotaState.Orange)
            awaitComplete()
        }
    }

    @Test
    fun `test that the state broadcast by a batch download is emitted`() = runTest {
        notificationsRepository.stub { on { monitorEvent() } doReturn emptyFlow() }
        transferRepository.stub {
            on { monitorTransferQuotaState() } doReturn flowOf(TransferQuotaState.Red)
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(TransferQuotaState.Red)
            awaitComplete()
        }
    }

    @Test
    fun `test that other global events are not emitted`() = runTest {
        notificationsRepository.stub {
            on { monitorEvent() } doReturn flowOf(
                StreamOverQuotaEvent(handle = 1L, timeLeft = 10.seconds)
            )
        }
        transferRepository.stub { on { monitorTransferQuotaState() } doReturn emptyFlow() }

        underTest().test {
            awaitComplete()
        }
    }
}

package mega.privacy.android.domain.usecase.transfers.overquota

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalTime::class)
class MonitorTransferOverQuotaErrorTimestampUseCaseTest {

    private lateinit var underTest: MonitorTransferOverQuotaErrorTimestampUseCase

    private val transfersRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorTransferOverQuotaErrorTimestampUseCase(transfersRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transfersRepository)
    }

    @Test
    fun `test that use case calls repository and emits`() = runTest {
        val timeStamp = Instant.fromEpochMilliseconds(1234)

        whenever(transfersRepository.monitorTransferOverQuotaErrorTimestamp())
            .thenReturn(flowOf(timeStamp))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(timeStamp)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
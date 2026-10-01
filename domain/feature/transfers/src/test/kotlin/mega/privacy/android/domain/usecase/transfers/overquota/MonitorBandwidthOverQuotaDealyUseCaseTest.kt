package mega.privacy.android.domain.usecase.transfers.overquota

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorBandwidthOverQuotaDelayUseCase.Companion.WAIT_TIME_TO_SHOW_TRANSFER_OVER_QUOTA_WARNING
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorBandwidthOverQuotaDelayUseCaseTest {

    private lateinit var underTest: MonitorBandwidthOverQuotaDelayUseCase

    private val transfersRepository = mock<TransferRepository>()
    private val getCurrentTimeInMillisUseCase = mock<GetCurrentTimeInMillisUseCase>()
    private val monitorTransferOverQuotaUseCase = mock<MonitorTransferOverQuotaUseCase>()
    private val getBandwidthOverQuotaDelayUseCase = mock<GetBandwidthOverQuotaDelayUseCase>()

    private val bandwidthDelay = 1000.seconds
    private val currentTime = 123456789L

    private val atomicLong = AtomicLong(0L)

    @BeforeAll
    fun setUp() {
        underTest = MonitorBandwidthOverQuotaDelayUseCase(
            transferRepository = transfersRepository,
            getCurrentTimeInMillisUseCase = getCurrentTimeInMillisUseCase,
            monitorTransferOverQuotaUseCase = monitorTransferOverQuotaUseCase,
            getBandwidthOverQuotaDelayUseCase = getBandwidthOverQuotaDelayUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transfersRepository,
            getCurrentTimeInMillisUseCase,
            monitorTransferOverQuotaUseCase,
            getBandwidthOverQuotaDelayUseCase
        )
    }

    @Test
    fun `test that if MonitorTransferOverQuotaUseCase emits false, this use case emits null`() =
        runTest {
            whenever(monitorTransferOverQuotaUseCase()) doReturn flowOf(false)
            whenever(transfersRepository.transferOverQuotaTimestamp) doReturn atomicLong

            underTest().test {
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that if transferOverQuotaTimestamp is already updated, this use case emits null`() =
        runTest {
            val updatedAtomicLong = AtomicLong(currentTime)

            whenever(monitorTransferOverQuotaUseCase()) doReturn flowOf(true)
            whenever(transfersRepository.transferOverQuotaTimestamp)
                .thenReturn(atomicLong, updatedAtomicLong)
            whenever(getCurrentTimeInMillisUseCase()) doReturn currentTime
            whenever(getBandwidthOverQuotaDelayUseCase()) doReturn bandwidthDelay

            underTest().test {
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest
    @ValueSource(longs = [0L, 123456780L, 113456789L])
    fun `test that use case emits correctly when transferOverQuotaTimestamp is null or old enough`(
        transferOverQuotaTimestamp: Long,
    ) = runTest {
        val atomicLong = AtomicLong(transferOverQuotaTimestamp)

        whenever(monitorTransferOverQuotaUseCase()) doReturn flowOf(true)
        whenever(transfersRepository.transferOverQuotaTimestamp) doReturn atomicLong
        whenever(getCurrentTimeInMillisUseCase()) doReturn currentTime
        whenever(getBandwidthOverQuotaDelayUseCase()) doReturn bandwidthDelay

        underTest().test {
            if ((currentTime - transferOverQuotaTimestamp) > WAIT_TIME_TO_SHOW_TRANSFER_OVER_QUOTA_WARNING) {
                assertThat(awaitItem()).isEqualTo(bandwidthDelay)
            } else {
                assertThat(awaitItem()).isNull()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
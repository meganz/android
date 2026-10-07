package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.IsInTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncProgressBlockersUseCaseTest {

    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val monitorStorageOverQuotaUseCase = mock<MonitorStorageOverQuotaUseCase>()
    private val monitorTransferOverQuotaUseCase = mock<MonitorTransferOverQuotaUseCase>()
    private val isInTransferOverQuotaUseCase = mock<IsInTransferOverQuotaUseCase>()
    private val monitorPausedTransfersUseCase = mock<MonitorPausedTransfersUseCase>()
    private val monitorSyncStalledIssuesUseCase = mock<MonitorSyncStalledIssuesUseCase>()

    private lateinit var underTest: MonitorSyncProgressBlockersUseCase

    @BeforeAll
    fun setUp() {
        underTest = MonitorSyncProgressBlockersUseCase(
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            monitorStorageOverQuotaUseCase = monitorStorageOverQuotaUseCase,
            monitorTransferOverQuotaUseCase = monitorTransferOverQuotaUseCase,
            isInTransferOverQuotaUseCase = isInTransferOverQuotaUseCase,
            monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
            monitorSyncStalledIssuesUseCase = monitorSyncStalledIssuesUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorConnectivityUseCase,
            monitorStorageOverQuotaUseCase,
            monitorTransferOverQuotaUseCase,
            isInTransferOverQuotaUseCase,
            monitorPausedTransfersUseCase,
            monitorSyncStalledIssuesUseCase,
        )
    }

    private class Inputs(
        val connectivity: MutableStateFlow<Boolean>,
        val storage: MutableStateFlow<Boolean>,
        val transferFlag: MutableStateFlow<Boolean>,
        val paused: MutableStateFlow<Boolean>,
        val issues: MutableStateFlow<List<StalledIssue>>,
    )

    private suspend fun stubInputs(
        connected: Boolean = true,
        storage: Boolean = false,
        transferFlag: Boolean = false,
        sdkTransfer: Boolean = false,
        paused: Boolean = false,
        issues: List<StalledIssue> = emptyList(),
    ): Inputs {
        val inputs = Inputs(
            connectivity = MutableStateFlow(connected),
            storage = MutableStateFlow(storage),
            transferFlag = MutableStateFlow(transferFlag),
            paused = MutableStateFlow(paused),
            issues = MutableStateFlow(issues),
        )
        whenever(monitorConnectivityUseCase()).thenReturn(inputs.connectivity)
        whenever(monitorStorageOverQuotaUseCase()).thenReturn(inputs.storage)
        whenever(monitorTransferOverQuotaUseCase()).thenReturn(inputs.transferFlag)
        whenever(monitorPausedTransfersUseCase()).thenReturn(inputs.paused)
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(inputs.issues)
        whenever(isInTransferOverQuotaUseCase()).thenReturn(sdkTransfer)
        return inputs
    }

    @Test
    fun `test that invoke emits a first value when every source has its current value`() =
        runTest {
            stubInputs()

            underTest().test {
                assertThat(awaitItem()).isEqualTo(
                    SyncProgressBlockers(
                        isOffline = false,
                        isStorageOverQuota = false,
                        isTransferOverQuota = false,
                        areTransfersPaused = false,
                        stalledIssueCount = 0,
                    )
                )
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that invoke reports offline when there is no connectivity`() = runTest {
        stubInputs(connected = false)

        underTest().test {
            assertThat(awaitItem().isOffline).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke reports online when there is connectivity`() = runTest {
        stubInputs(connected = true)

        underTest().test {
            assertThat(awaitItem().isOffline).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke reports storage over quota`() = runTest {
        stubInputs(storage = true)

        underTest().test {
            assertThat(awaitItem().isStorageOverQuota).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke reports transfer over quota when the sdk is over quota and the flag is false`() =
        runTest {
            stubInputs(transferFlag = false, sdkTransfer = true)

            underTest().test {
                assertThat(awaitItem().isTransferOverQuota).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that invoke reports transfer over quota when the flag is true without reading the sdk`() =
        runTest {
            stubInputs(transferFlag = true)

            underTest().test {
                assertThat(awaitItem().isTransferOverQuota).isTrue()
                verify(isInTransferOverQuotaUseCase, never()).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that invoke does not report transfer over quota when neither the flag nor the sdk is over quota`() =
        runTest {
            stubInputs(transferFlag = false, sdkTransfer = false)

            underTest().test {
                assertThat(awaitItem().isTransferOverQuota).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that invoke re-reads the sdk transfer over quota state when the flag emits again`() =
        runTest {
            val inputs = stubInputs(transferFlag = false, sdkTransfer = false)

            underTest().test {
                assertThat(awaitItem().isTransferOverQuota).isFalse()

                whenever(isInTransferOverQuotaUseCase()).thenReturn(true)
                inputs.transferFlag.value = true
                assertThat(awaitItem().isTransferOverQuota).isTrue()

                inputs.transferFlag.value = false
                expectNoEvents()
                verify(isInTransferOverQuotaUseCase, times(2)).invoke()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that invoke does not report transfer over quota when the sdk read fails`() =
        runTest {
            stubInputs(transferFlag = false)
            whenever(isInTransferOverQuotaUseCase()).thenThrow(RuntimeException("boom"))

            underTest().test {
                assertThat(awaitItem().isTransferOverQuota).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that invoke reports paused transfers`() = runTest {
        stubInputs(paused = true)

        underTest().test {
            assertThat(awaitItem().areTransfersPaused).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke reports the stalled issue count`() = runTest {
        val inputs = stubInputs(issues = List(3) { mock<StalledIssue>() })

        underTest().test {
            assertThat(awaitItem().stalledIssueCount).isEqualTo(3)

            inputs.issues.value = emptyList()
            assertThat(awaitItem().stalledIssueCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke does not emit a duplicate value`() = runTest {
        val inputs = stubInputs(issues = List(2) { mock<StalledIssue>() })

        underTest().test {
            assertThat(awaitItem().stalledIssueCount).isEqualTo(2)

            inputs.issues.value = List(2) { mock<StalledIssue>() }
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

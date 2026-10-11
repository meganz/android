package mega.privacy.android.domain.usecase.transfers.offline

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.pending.GetPendingTransfersByTypeUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorOfflineDownloadInProgressUseCaseTest {
    private lateinit var underTest: MonitorOfflineDownloadInProgressUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getPendingTransfersByTypeUseCase = mock<GetPendingTransfersByTypeUseCase>()
    private val isOfflineDownloadInProgressUseCase = mock<IsOfflineDownloadInProgressUseCase>()

    private val pendingTransfers = MutableStateFlow(emptyList<PendingTransfer>())
    private val activeTransfers = MutableStateFlow(emptyList<ActiveTransfer>())

    private val node = mock<TypedFolderNode> { on { id } doReturn NodeId(1L) }

    @BeforeAll
    fun setup() {
        underTest = MonitorOfflineDownloadInProgressUseCase(
            transferRepository = transferRepository,
            getPendingTransfersByTypeUseCase = getPendingTransfersByTypeUseCase,
            isOfflineDownloadInProgressUseCase = isOfflineDownloadInProgressUseCase,
        )
    }

    @BeforeEach
    fun cleanup() = runTest {
        reset(
            transferRepository,
            getPendingTransfersByTypeUseCase,
            isOfflineDownloadInProgressUseCase,
        )
        pendingTransfers.value = emptyList()
        activeTransfers.value = emptyList()
        whenever(getPendingTransfersByTypeUseCase(TransferType.DOWNLOAD)) doReturn pendingTransfers
        whenever(transferRepository.monitorActiveTransfersByType(TransferType.DOWNLOAD)) doReturn
                activeTransfers
        whenever(isOfflineDownloadInProgressUseCase(node)).thenReturn(false)
    }

    @Test
    fun `test that invoke emits the current state of the node`() = runTest {
        whenever(isOfflineDownloadInProgressUseCase(node)).thenReturn(true)

        underTest(node).test {
            assertThat(awaitItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits again when the state of the node changes`() = runTest {
        underTest(node).test {
            assertThat(awaitItem()).isFalse()

            whenever(isOfflineDownloadInProgressUseCase(node)).thenReturn(true)
            activeTransfers.value = listOf(mock())

            assertThat(awaitItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke does not emit again when the state of the node does not change`() =
        runTest {
            underTest(node).test {
                assertThat(awaitItem()).isFalse()

                activeTransfers.value = listOf(mock())
                pendingTransfers.value = listOf(mock())

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that invoke only monitors download transfers`() = runTest {
        underTest(node).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify(getPendingTransfersByTypeUseCase).invoke(TransferType.DOWNLOAD)
        verify(transferRepository).monitorActiveTransfersByType(TransferType.DOWNLOAD)
    }

}

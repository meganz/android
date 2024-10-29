package mega.privacy.android.domain.usecase.transfers

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.TransfersStatusInfo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorLastTransfersHaveBeenCancelledUseCaseTest {
    private lateinit var underTest: MonitorLastTransfersHaveBeenCancelledUseCase

    private val monitorTransfersStatusUseCase = mock<MonitorTransfersStatusUseCase>()

    @BeforeAll
    fun setup() {
        underTest = MonitorLastTransfersHaveBeenCancelledUseCase(monitorTransfersStatusUseCase)
    }

    @BeforeEach
    fun cleanUp() = reset(monitorTransfersStatusUseCase)

    @ParameterizedTest
    @MethodSource("provideTransferStatusInfoWithPendingUploads")
    fun `test that flow emits when there were ongoing active transfers and then all are cancelled`(
        transfersStatusInfo: TransfersStatusInfo,
    ) = runTest {
        whenever(monitorTransfersStatusUseCase()) doReturn flowOf(
            transfersStatusInfo,
            TransfersStatusInfo(cancelled = transfersStatusInfo.pendingDownloads + transfersStatusInfo.pendingUploads)
        )
        underTest().test {
            awaitItem()
            awaitComplete()
        }
    }

    @ParameterizedTest
    @MethodSource("provideTransferStatusInfoWithPendingUploads")
    fun `test that flow does not emit when there were ongoing active transfers and then all are finished normally`(
        transfersStatusInfo: TransfersStatusInfo,
    ) = runTest {
        whenever(monitorTransfersStatusUseCase()) doReturn flowOf(
            transfersStatusInfo,
            TransfersStatusInfo()
        )
        underTest().test {
            awaitComplete()
        }
    }

    @ParameterizedTest
    @MethodSource("provideTransferStatusInfoWithPendingUploads")
    fun `test that flow does not emit when there were ongoing active transfers and then all are finished but not all cancelled`(
        transfersStatusInfo: TransfersStatusInfo,
    ) = runTest {
        whenever(monitorTransfersStatusUseCase()) doReturn flowOf(
            transfersStatusInfo,
            TransfersStatusInfo(cancelled = transfersStatusInfo.pendingDownloads + transfersStatusInfo.pendingUploads - 1)
        )
        underTest().test {
            awaitComplete()
        }
    }

    @ParameterizedTest
    @MethodSource("provideTransferStatusInfoWithPendingUploads")
    fun `test that flow does not emit when there were ongoing active transfers and some are cancelled but there are still some ongoing transfers`(
        transfersStatusInfo: TransfersStatusInfo,
    ) = runTest {
        whenever(monitorTransfersStatusUseCase()) doReturn flowOf(
            transfersStatusInfo,
            transfersStatusInfo.copy(
                pendingUploads = transfersStatusInfo.pendingUploads - 1,
                cancelled = transfersStatusInfo.pendingDownloads + transfersStatusInfo.pendingUploads - 1
            )
        )
        underTest().test {
            awaitComplete()
        }
    }

    private fun provideTransferStatusInfoWithPendingUploads() = listOf(
        TransfersStatusInfo(pendingUploads = 1),
        TransfersStatusInfo(pendingUploads = 56),
        TransfersStatusInfo(pendingDownloads = 1),
        TransfersStatusInfo(pendingDownloads = 34),
        TransfersStatusInfo(pendingUploads = 1, pendingDownloads = 1),
        TransfersStatusInfo(pendingUploads = 1, pendingDownloads = 86),
        TransfersStatusInfo(pendingUploads = 76, pendingDownloads = 1),
        TransfersStatusInfo(pendingUploads = 76, pendingDownloads = 17),
    )

}
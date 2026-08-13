package mega.privacy.android.app.appstate.global.initialisation.appstart

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaEventQueue
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaSource
import mega.privacy.android.domain.entity.transfer.TransferOverQuotaStatus
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaEventUseCase
import mega.privacy.android.domain.usecase.transfers.previews.CancelOverQuotaPreviewDownloadsUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class DownloadOverQuotaInitialiserTest {

    private lateinit var underTest: DownloadOverQuotaInitialiser

    private val monitorTransferOverQuotaEventUseCase = mock<MonitorTransferOverQuotaEventUseCase>()
    private val cancelOverQuotaPreviewDownloadsUseCase =
        mock<CancelOverQuotaPreviewDownloadsUseCase>()
    private val broadcastTransferOverQuotaUseCase = mock<BroadcastTransferOverQuotaUseCase>()
    private val transferOverQuotaEventQueue = TransferOverQuotaEventQueue()

    private suspend fun initTest(status: TransferOverQuotaStatus) {
        monitorTransferOverQuotaEventUseCase.stub {
            on { invoke() } doReturn flowOf(status)
        }
        cancelOverQuotaPreviewDownloadsUseCase.stub {
            onBlocking { invoke() } doReturn emptyList()
        }
        underTest = DownloadOverQuotaInitialiser(
            monitorTransferOverQuotaEventUseCase = monitorTransferOverQuotaEventUseCase,
            cancelOverQuotaPreviewDownloadsUseCase = cancelOverQuotaPreviewDownloadsUseCase,
            broadcastTransferOverQuotaUseCase = broadcastTransferOverQuotaUseCase,
            transferOverQuotaEventQueue = transferOverQuotaEventQueue,
        )
    }

    @Test
    fun `test that the warning is queued as a download when an over quota event is received`() =
        runTest {
            initTest(TransferOverQuotaStatus.OverQuota)

            underTest()

            assertThat(transferOverQuotaEventQueue.consume())
                .isEqualTo(TransferOverQuotaSource.Download)
        }

    @Test
    fun `test that preview downloads are cancelled when an over quota event is received`() =
        runTest {
            initTest(TransferOverQuotaStatus.OverQuota)

            underTest()

            verify(cancelOverQuotaPreviewDownloadsUseCase).invoke()
        }

    @Test
    fun `test that preview downloads are not cancelled when an almost over quota event is received`() =
        runTest {
            initTest(TransferOverQuotaStatus.AlmostOverQuota)

            underTest()

            verifyNoInteractions(cancelOverQuotaPreviewDownloadsUseCase)
        }

    @Test
    fun `test that over quota is broadcast when an over quota event is received`() = runTest {
        initTest(TransferOverQuotaStatus.OverQuota)

        underTest()

        verify(broadcastTransferOverQuotaUseCase).invoke(true)
    }

    @Test
    fun `test that over quota is not broadcast when an almost over quota event is received`() =
        runTest {
            initTest(TransferOverQuotaStatus.AlmostOverQuota)

            underTest()

            verifyNoInteractions(broadcastTransferOverQuotaUseCase)
        }

    @Test
    fun `test that over quota is broadcast before the warning is queued`() = runTest {
        var alreadyQueued: TransferOverQuotaSource? = null
        broadcastTransferOverQuotaUseCase.stub {
            onBlocking { invoke(true) } doAnswer {
                alreadyQueued = transferOverQuotaEventQueue.consume()
            }
        }
        initTest(TransferOverQuotaStatus.OverQuota)

        underTest()

        assertThat(alreadyQueued).isNull()
    }
}

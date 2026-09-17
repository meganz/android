package mega.privacy.android.app.appstate.global.initialisation.appstart

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaEventQueue
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaSource
import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import mega.privacy.android.domain.usecase.transfers.overquota.IsInTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.IsStreamingMediaContentUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferQuotaStateEventUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class TransferQuotaStateInitialiserTest {

    private lateinit var underTest: TransferQuotaStateInitialiser

    private val monitorTransferQuotaStateEventUseCase =
        mock<MonitorTransferQuotaStateEventUseCase>()
    private val isStreamingMediaContentUseCase = mock<IsStreamingMediaContentUseCase>()
    private val isInTransferOverQuotaUseCase = mock<IsInTransferOverQuotaUseCase>()
    private val transferOverQuotaEventQueue = TransferOverQuotaEventQueue()

    private fun initTest(
        state: TransferQuotaState,
        isStreamingMedia: Boolean = false,
        isInOverQuota: Boolean = false,
    ) {
        monitorTransferQuotaStateEventUseCase.stub {
            on { invoke() } doReturn flowOf(state)
        }
        isStreamingMediaContentUseCase.stub {
            onBlocking { invoke() } doReturn isStreamingMedia
        }
        isInTransferOverQuotaUseCase.stub {
            onBlocking { invoke() } doReturn isInOverQuota
        }
        underTest = TransferQuotaStateInitialiser(
            monitorTransferQuotaStateEventUseCase = monitorTransferQuotaStateEventUseCase,
            isStreamingMediaContentUseCase = isStreamingMediaContentUseCase,
            isInTransferOverQuotaUseCase = isInTransferOverQuotaUseCase,
            transferOverQuotaEventQueue = transferOverQuotaEventQueue,
        )
    }

    @Test
    fun `test that a warning is queued as a download when the quota is running low`() = runTest {
        initTest(TransferQuotaState.Orange)

        underTest()

        assertThat(transferOverQuotaEventQueue.consume())
            .isEqualTo(TransferOverQuotaSource.Download)
    }

    @Test
    fun `test that a warning is queued as streaming when media is being streamed`() = runTest {
        initTest(TransferQuotaState.Orange, isStreamingMedia = true)

        underTest()

        assertThat(transferOverQuotaEventQueue.consume())
            .isEqualTo(TransferOverQuotaSource.Streaming)
    }

    @Test
    fun `test that no warning is queued when the quota is already exceeded`() = runTest {
        initTest(TransferQuotaState.Orange, isInOverQuota = true)

        underTest()

        assertThat(transferOverQuotaEventQueue.consume()).isNull()
    }

    @Test
    fun `test that no warning is queued when there is enough quota`() = runTest {
        initTest(TransferQuotaState.Green)

        underTest()

        assertThat(transferOverQuotaEventQueue.consume()).isNull()
    }

    @Test
    fun `test that no warning is queued when the quota cannot cover what is queued`() = runTest {
        initTest(TransferQuotaState.Red)

        underTest()

        assertThat(transferOverQuotaEventQueue.consume()).isNull()
    }

    @Test
    fun `test that no warning is queued when the state could not be determined`() = runTest {
        initTest(TransferQuotaState.Unknown)

        underTest()

        assertThat(transferOverQuotaEventQueue.consume()).isNull()
    }
}

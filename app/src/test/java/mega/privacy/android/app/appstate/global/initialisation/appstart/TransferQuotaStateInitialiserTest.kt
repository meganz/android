package mega.privacy.android.app.appstate.global.initialisation.appstart

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaEventQueue
import mega.privacy.android.app.appstate.global.quota.TransferOverQuotaSource
import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import mega.privacy.android.domain.entity.transfer.TransferQuotaWarningTrigger
import mega.privacy.android.domain.usecase.transfers.overquota.IsInTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.IsStreamingMediaContentUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferQuotaStateEventUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.SetTransferQuotaWarningShownUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.ShouldShowTransferQuotaWarningUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class TransferQuotaStateInitialiserTest {

    private lateinit var underTest: TransferQuotaStateInitialiser

    private val monitorTransferQuotaStateEventUseCase =
        mock<MonitorTransferQuotaStateEventUseCase>()
    private val isStreamingMediaContentUseCase = mock<IsStreamingMediaContentUseCase>()
    private val isInTransferOverQuotaUseCase = mock<IsInTransferOverQuotaUseCase>()
    private val shouldShowTransferQuotaWarningUseCase =
        mock<ShouldShowTransferQuotaWarningUseCase>()
    private val setTransferQuotaWarningShownUseCase = mock<SetTransferQuotaWarningShownUseCase>()
    private val transferOverQuotaEventQueue = TransferOverQuotaEventQueue()

    private fun initTest(
        state: TransferQuotaState,
        isStreamingMedia: Boolean = false,
        isInOverQuota: Boolean = false,
        shouldShowWarning: Boolean = true,
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
        shouldShowTransferQuotaWarningUseCase.stub {
            onBlocking { invoke(any()) } doReturn shouldShowWarning
        }
        underTest = TransferQuotaStateInitialiser(
            monitorTransferQuotaStateEventUseCase = monitorTransferQuotaStateEventUseCase,
            isStreamingMediaContentUseCase = isStreamingMediaContentUseCase,
            isInTransferOverQuotaUseCase = isInTransferOverQuotaUseCase,
            shouldShowTransferQuotaWarningUseCase = shouldShowTransferQuotaWarningUseCase,
            setTransferQuotaWarningShownUseCase = setTransferQuotaWarningShownUseCase,
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

    @Test
    fun `test that no warning is queued when it has already been shown today`() = runTest {
        initTest(TransferQuotaState.Orange, shouldShowWarning = false)

        underTest()

        assertThat(transferOverQuotaEventQueue.consume()).isNull()
        verifyNoInteractions(setTransferQuotaWarningShownUseCase)
    }

    @Test
    fun `test that the download allowance is spent when a download warning is queued`() = runTest {
        initTest(TransferQuotaState.Orange)

        underTest()

        verify(setTransferQuotaWarningShownUseCase)(TransferQuotaWarningTrigger.Download)
    }

    @Test
    fun `test that the streaming allowance is spent when a streaming warning is queued`() =
        runTest {
            initTest(TransferQuotaState.Orange, isStreamingMedia = true)

            underTest()

            verify(setTransferQuotaWarningShownUseCase)(TransferQuotaWarningTrigger.Streaming)
        }

    @Test
    fun `test that no allowance is spent when the quota is not running low`() = runTest {
        initTest(TransferQuotaState.Green)

        underTest()

        verify(shouldShowTransferQuotaWarningUseCase, never())(any())
        verifyNoInteractions(setTransferQuotaWarningShownUseCase)
    }
}

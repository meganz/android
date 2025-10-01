package mega.privacy.android.app.presentation.photos.timeline.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.photos.model.CameraUploadsTransferType
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.transfers.active.MonitorCameraUploadsInProgressTransfersUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.math.BigInteger

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraUploadsTransferViewModelTest {
    private lateinit var underTest: CameraUploadsTransferViewModel

    private val monitorCameraUploadsInProgressTransfersUseCase =
        mock<MonitorCameraUploadsInProgressTransfersUseCase>()

    fun initViewModel() {
        underTest = CameraUploadsTransferViewModel(
            monitorCameraUploadsInProgressTransfersUseCase = monitorCameraUploadsInProgressTransfersUseCase
        )
    }

    @AfterEach
    fun tearDownEach() {
        reset(monitorCameraUploadsInProgressTransfersUseCase)
    }

    @Test
    fun `test that cameraUploadsTransfers is updated correctly`() = runTest {
        val flow = MutableStateFlow<Map<Long, InProgressTransfer>>(emptyMap())
        val transfer1 = mock<InProgressTransfer.Upload> {
            on { priority }.thenReturn(BigInteger.ONE)
            on { state }.thenReturn(TransferState.STATE_ACTIVE)
        }
        val transfer2 = mock<InProgressTransfer.Upload> {
            on { priority }.thenReturn(BigInteger.TWO)
            on { state }.thenReturn(TransferState.STATE_QUEUED)
        }
        val map = mapOf(1L to transfer1, 2L to transfer2)

        whenever(monitorCameraUploadsInProgressTransfersUseCase()).thenReturn(flow)

        initViewModel()
        advanceUntilIdle()

        underTest.cameraUploadsTransfers.test {
            val initialItem = awaitItem()
            assertThat(initialItem).isEmpty()

            flow.value = map

            val actual = awaitItem()
            assertThat(actual).isNotEmpty()
            assertThat(actual.size).isEqualTo(2)
            actual.forEach { item ->
                if (item is CameraUploadsTransferType.InProgress) {
                    assertThat(item.items).containsExactly(transfer1)
                }
                if (item is CameraUploadsTransferType.InQueue) {
                    assertThat(item.items).containsExactly(transfer2)
                }
            }
        }
    }
}
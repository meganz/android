package mega.privacy.android.app.presentation.provider

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileProviderViewModelTest {

    private lateinit var underTest: FileProviderViewModel

    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase = mock()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = FileProviderViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase,
            stopCameraUploadsUseCase,
        )
    }

    @Test
    fun `test that stopCameraUploads calls stopCameraUploadsUseCase with restartMode StopAndDisable`() =
        runTest {
            underTest.stopCameraUploads()
            verify(stopCameraUploadsUseCase).invoke(restartMode = CameraUploadsRestartMode.StopAndDisable)
        }

    @Test
    fun `test that stopCameraUploads catch the error if stopCameraUploadsUseCase throws an error`() =
        runTest {
            whenever(stopCameraUploadsUseCase(CameraUploadsRestartMode.StopAndDisable))
                .thenThrow(RuntimeException::class.java)

            assertDoesNotThrow { underTest.stopCameraUploads() }
        }

    @Test
    fun `test that state is updated with the correct event when start download is invoked`() =
        runTest {
            val id = NodeId(15L)
            val node = mock<TypedFileNode>()
            whenever(getNodeByIdUseCase(id)) doReturn node
            val expected = triggered(TransferTriggerEvent.StartDownloadNode(listOf(node), true))

            underTest.uiState.test {
                awaitItem()
                underTest.startDownload(listOf(id))
                val actual = awaitItem().startDownloadEvent
                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that state event is consumed when consume transfer trigger event is invoked`() =
        runTest {
            val id = NodeId(15L)
            val node = mock<TypedFileNode>()
            whenever(getNodeByIdUseCase(id)) doReturn node
            val expected = consumed()

            underTest.uiState.test {
                awaitItem()
                underTest.startDownload(listOf(id))
                assertThat(awaitItem().startDownloadEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                underTest.consumeTransferTriggerEvent()
                val actual = awaitItem().startDownloadEvent
                assertThat(actual).isEqualTo(expected)
            }
        }
}

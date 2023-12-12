package mega.privacy.android.app.presentation.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
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

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = FileProviderViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
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
}

package mega.privacy.android.app.initializer

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import mega.privacy.android.domain.usecase.environment.MonitorDevicePowerConnectionStateUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraUploadAutoStartInitializerTest {
    private lateinit var underTest: CameraUploadAutoStartInitializer

    private val monitorDevicePowerConnectionStateUseCase =
        mock<MonitorDevicePowerConnectionStateUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val monitorDevicePowerConnectionFakeFlow =
        MutableSharedFlow<DevicePowerConnectionState>()
    private val monitorConnectivityFakeFlow = MutableSharedFlow<Boolean>()

    @BeforeAll
    fun setup() {
        whenever(monitorDevicePowerConnectionStateUseCase()).thenReturn(
            monitorDevicePowerConnectionFakeFlow
        )
        whenever(monitorConnectivityUseCase()).thenReturn(monitorConnectivityFakeFlow)
        underTest = CameraUploadAutoStartInitializer(
            monitorDevicePowerConnectionStateUseCase,
            monitorConnectivityUseCase,
            startCameraUploadUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(startCameraUploadUseCase)
    }

    @Test
    fun `test that camera uploads automatically starts when the device begins charging`() =
        runTest {
            val job = launch { underTest("session", false) }
            testScheduler.advanceUntilIdle()
            monitorDevicePowerConnectionFakeFlow.emit(DevicePowerConnectionState.Connected)
            testScheduler.advanceUntilIdle()

            verify(startCameraUploadUseCase).invoke()
            job.cancel()
        }

    @Test
    fun `test that camera uploads does not automatically start when the device is not charging`() =
        runTest {
            val job = launch { underTest("session", false) }
            testScheduler.advanceUntilIdle()
            monitorDevicePowerConnectionFakeFlow.emit(DevicePowerConnectionState.Disconnected)
            testScheduler.advanceUntilIdle()

            verifyNoInteractions(startCameraUploadUseCase)
            job.cancel()
        }

    @Test
    fun `test that camera uploads does not automatically start when the device charging state is unknown`() =
        runTest {
            val job = launch { underTest("session", false) }
            testScheduler.advanceUntilIdle()
            monitorDevicePowerConnectionFakeFlow.emit(DevicePowerConnectionState.Unknown)
            testScheduler.advanceUntilIdle()

            verifyNoInteractions(startCameraUploadUseCase)
            job.cancel()
        }

    @Test
    fun `test that camera uploads automatically starts when the device regains connectivity`() =
        runTest {
            val job = launch { underTest("session", false) }
            testScheduler.advanceUntilIdle()
            monitorConnectivityFakeFlow.emit(true)
            testScheduler.advanceUntilIdle()

            verify(startCameraUploadUseCase).invoke()
            job.cancel()
        }

    @Test
    fun `test that camera uploads does not automatically start when the device is offline`() =
        runTest {
            val job = launch { underTest("session", false) }
            testScheduler.advanceUntilIdle()
            monitorConnectivityFakeFlow.emit(false)
            testScheduler.advanceUntilIdle()

            verifyNoInteractions(startCameraUploadUseCase)
            job.cancel()
        }
}

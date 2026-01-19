package mega.privacy.android.app.initializer

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import mega.privacy.android.domain.usecase.environment.MonitorDevicePowerConnectionStateUseCase
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
class MonitorDevicePowerAndStartCameraUploadInitializerTest {
    private lateinit var underTest: MonitorDevicePowerAndStartCameraUploadInitializer

    private val monitorDevicePowerConnectionStateUseCase =
        mock<MonitorDevicePowerConnectionStateUseCase>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val monitorDevicePowerConnectionFakeFlow =
        MutableSharedFlow<DevicePowerConnectionState>()

    @BeforeAll
    fun setup() {
        whenever(monitorDevicePowerConnectionStateUseCase()).thenReturn(
            monitorDevicePowerConnectionFakeFlow
        )
        underTest = MonitorDevicePowerAndStartCameraUploadInitializer()
    }

    @BeforeEach
    fun cleanUp() {
        reset(startCameraUploadUseCase)
    }

    @Test
    fun `test that camera uploads automatically starts when the device begins charging`() =
        runTest {
            val job = launch {
                underTest.action(
                    monitorDevicePowerConnectionStateUseCase,
                    startCameraUploadUseCase,
                )
            }
            testScheduler.advanceUntilIdle()
            monitorDevicePowerConnectionFakeFlow.emit(DevicePowerConnectionState.Connected)

            verify(startCameraUploadUseCase).invoke()
            job.cancel()
        }

    @Test
    fun `test that camera uploads does not automatically start when the device is not charging`() =
        runTest {
            val job = launch {
                underTest.action(
                    monitorDevicePowerConnectionStateUseCase,
                    startCameraUploadUseCase,
                )
            }
            testScheduler.advanceUntilIdle()
            monitorDevicePowerConnectionFakeFlow.emit(DevicePowerConnectionState.Disconnected)

            verifyNoInteractions(startCameraUploadUseCase)
            job.cancel()
        }

    @Test
    fun `test that camera uploads does not automatically start when the device charging state is unknown`() =
        runTest {
            val job = launch {
                underTest.action(
                    monitorDevicePowerConnectionStateUseCase,
                    startCameraUploadUseCase,
                )
            }
            testScheduler.advanceUntilIdle()
            monitorDevicePowerConnectionFakeFlow.emit(DevicePowerConnectionState.Unknown)

            verifyNoInteractions(startCameraUploadUseCase)
            job.cancel()
        }
}

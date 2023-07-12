package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFolderState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsState
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateCameraUploadsBackupHeartbeatStatusUseCaseTest {
    lateinit var underTest: UpdateCameraUploadsBackupHeartbeatStatusUseCase

    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()
    private val reportUploadStatusUseCase = mock<ReportUploadStatusUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateCameraUploadsBackupHeartbeatStatusUseCase(
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            reportUploadStatusUseCase = reportUploadStatusUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isCameraUploadsEnabledUseCase,
            isSecondaryFolderEnabled,
            reportUploadStatusUseCase,
        )
    }

    @Test
    fun `test that if camera uploads is not enabled then primary and secondary back up state is not reported`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)

            val heartbeatStatus = mock<HeartbeatStatus>()
            val cameraUploadsState = mock<CameraUploadsState>()

            underTest.invoke(heartbeatStatus, cameraUploadsState)
            verifyNoInteractions(reportUploadStatusUseCase)
        }

    @Test
    fun `test that if camera uploads is enabled then primary back up state is reported`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(isSecondaryFolderEnabled()).thenReturn(false)

            val heartbeatStatus = HeartbeatStatus.UP_TO_DATE
            val cameraUploadsState = CameraUploadsState(
                primaryCameraUploadsState = CameraUploadsFolderState(
                    toUploadCount = 10,
                    uploadedCount = 9,
                    lastHandle = 1L,
                    lastTimestamp = 2L
                )
            )

            underTest.invoke(heartbeatStatus, cameraUploadsState)
            verify(reportUploadStatusUseCase).invoke(
                cameraUploadFolderType = CameraUploadFolderType.Primary,
                heartbeatStatus = heartbeatStatus,
                pendingUploads = cameraUploadsState.primaryCameraUploadsState.pendingCount,
                lastNodeHandle = cameraUploadsState.primaryCameraUploadsState.lastHandle,
                lastTimestamp = cameraUploadsState.primaryCameraUploadsState.lastTimestamp,
            )
        }

    @Test
    fun `test that if secondary folder is not enabled then secondary back up state is reported`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(isSecondaryFolderEnabled()).thenReturn(false)
            val heartbeatStatus = HeartbeatStatus.UP_TO_DATE
            val cameraUploadsState = CameraUploadsState(
                secondaryCameraUploadsState = CameraUploadsFolderState(
                    toUploadCount = 10,
                    uploadedCount = 9,
                    lastHandle = 1L,
                    lastTimestamp = 2L
                )
            )
            underTest.invoke(heartbeatStatus, cameraUploadsState)
            verify(reportUploadStatusUseCase, never()).invoke(
                cameraUploadFolderType = CameraUploadFolderType.Secondary,
                heartbeatStatus = heartbeatStatus,
                pendingUploads = cameraUploadsState.secondaryCameraUploadsState.pendingCount,
                lastNodeHandle = cameraUploadsState.secondaryCameraUploadsState.lastHandle,
                lastTimestamp = cameraUploadsState.secondaryCameraUploadsState.lastTimestamp,
            )
        }

    @Test
    fun `test that if secondary folder is enabled then secondary back up state is reported`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            val heartbeatStatus = mock<HeartbeatStatus>()
            val cameraUploadsState = CameraUploadsState(
                secondaryCameraUploadsState = CameraUploadsFolderState(
                    toUploadCount = 10,
                    uploadedCount = 9,
                    lastHandle = 1L,
                    lastTimestamp = 2L
                )
            )
            underTest.invoke(heartbeatStatus, cameraUploadsState)
            verify(reportUploadStatusUseCase).invoke(
                cameraUploadFolderType = CameraUploadFolderType.Secondary,
                heartbeatStatus = heartbeatStatus,
                pendingUploads = cameraUploadsState.secondaryCameraUploadsState.pendingCount,
                lastNodeHandle = cameraUploadsState.secondaryCameraUploadsState.lastHandle,
                lastTimestamp = cameraUploadsState.secondaryCameraUploadsState.lastTimestamp,
            )
        }
}

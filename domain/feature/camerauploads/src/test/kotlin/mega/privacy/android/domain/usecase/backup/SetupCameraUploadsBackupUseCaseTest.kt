package mega.privacy.android.domain.usecase.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendCameraUploadsBackupHeartBeatUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [SetupCameraUploadsBackupUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetupCameraUploadsBackupUseCaseTest {

    private lateinit var underTest: SetupCameraUploadsBackupUseCase

    private val setBackupUseCase = mock<SetBackupUseCase>()
    private val getPrimarySyncHandleUseCase = mock<GetPrimarySyncHandleUseCase>()
    private val getPrimaryFolderPathUseCase = mock<GetPrimaryFolderPathUseCase>()
    private val broadcastBackupInfoTypeUseCase = mock<BroadcastBackupInfoTypeUseCase>()
    private val sendCameraUploadsBackupHeartBeatUseCase =
        mock<SendCameraUploadsBackupHeartBeatUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetupCameraUploadsBackupUseCase(
            setBackupUseCase = setBackupUseCase,
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            getPrimaryFolderPathUseCase = getPrimaryFolderPathUseCase,
            broadcastBackupInfoTypeUseCase = broadcastBackupInfoTypeUseCase,
            sendCameraUploadsBackupHeartBeatUseCase = sendCameraUploadsBackupHeartBeatUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            setBackupUseCase,
            getPrimarySyncHandleUseCase,
            getPrimaryFolderPathUseCase,
            broadcastBackupInfoTypeUseCase,
            sendCameraUploadsBackupHeartBeatUseCase,
        )
    }

    @Test
    fun `test that backup is not set when camera upload has invalid handle`() = runTest {
        whenever(getPrimarySyncHandleUseCase()).thenReturn(-1)
        underTest("Camera Uploads")
        verify(broadcastBackupInfoTypeUseCase).invoke(BackupInfoType.CAMERA_UPLOADS)
        verifyNoInteractions(
            getPrimaryFolderPathUseCase,
            setBackupUseCase,
            sendCameraUploadsBackupHeartBeatUseCase
        )
    }

    @Test
    fun `test that backup is not set when camera upload has invalid path`() = runTest {
        whenever(getPrimarySyncHandleUseCase()).thenReturn(123L)
        whenever(getPrimaryFolderPathUseCase()).thenReturn("")
        underTest("Camera Uploads")
        verify(broadcastBackupInfoTypeUseCase).invoke(BackupInfoType.CAMERA_UPLOADS)
        verifyNoInteractions(
            setBackupUseCase,
            sendCameraUploadsBackupHeartBeatUseCase
        )
    }

    @Test
    fun `test that backup is set when local camera upload is set`() = runTest {
        val handle = 123L
        val localFolder = "/path/to/camera/upload"
        val backupName = "Camera Uploads"
        whenever(getPrimarySyncHandleUseCase()).thenReturn(handle)
        whenever(getPrimaryFolderPathUseCase()).thenReturn(localFolder)
        underTest(backupName)
        verify(setBackupUseCase).invoke(
            backupType = BackupInfoType.CAMERA_UPLOADS,
            targetNode = handle,
            localFolder = localFolder,
            backupName = backupName,
            state = BackupState.ACTIVE,
        )
        verify(broadcastBackupInfoTypeUseCase).invoke(BackupInfoType.CAMERA_UPLOADS)
        verify(sendCameraUploadsBackupHeartBeatUseCase).invoke(
            heartbeatStatus = HeartbeatStatus.UNKNOWN,
            lastNodeHandle = -1
        )
    }
}

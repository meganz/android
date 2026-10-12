package mega.privacy.android.domain.usecase.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendMediaUploadsBackupHeartBeatUseCase
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
 * Test class for [SetupMediaUploadsBackupUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetupMediaUploadsBackupUseCaseTest {

    private lateinit var underTest: SetupMediaUploadsBackupUseCase

    private val setBackupUseCase = mock<SetBackupUseCase>()
    private val getSecondarySyncHandleUseCase = mock<GetSecondarySyncHandleUseCase>()
    private val getSecondaryFolderPathUseCase = mock<GetSecondaryFolderPathUseCase>()
    private val broadcastBackupInfoTypeUseCase = mock<BroadcastBackupInfoTypeUseCase>()
    private val sendMediaUploadsBackupHeartBeatUseCase =
        mock<SendMediaUploadsBackupHeartBeatUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetupMediaUploadsBackupUseCase(
            setBackupUseCase = setBackupUseCase,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase,
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
            broadcastBackupInfoTypeUseCase = broadcastBackupInfoTypeUseCase,
            sendMediaUploadsBackupHeartBeatUseCase = sendMediaUploadsBackupHeartBeatUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            setBackupUseCase,
            getSecondarySyncHandleUseCase,
            getSecondaryFolderPathUseCase,
            broadcastBackupInfoTypeUseCase,
            sendMediaUploadsBackupHeartBeatUseCase,
        )
    }

    @Test
    fun `test that backup is not set when media uploads has invalid handle`() = runTest {
        whenever(getSecondarySyncHandleUseCase()).thenReturn(-1)
        underTest("Media Uploads")
        verify(broadcastBackupInfoTypeUseCase).invoke(BackupInfoType.MEDIA_UPLOADS)
        verifyNoInteractions(
            getSecondaryFolderPathUseCase,
            setBackupUseCase,
            sendMediaUploadsBackupHeartBeatUseCase
        )
    }

    @Test
    fun `test that backup is not set when media uploads has invalid path`() = runTest {
        whenever(getSecondarySyncHandleUseCase()).thenReturn(123L)
        whenever(getSecondaryFolderPathUseCase()).thenReturn("")
        underTest("Media Uploads")
        verify(broadcastBackupInfoTypeUseCase).invoke(BackupInfoType.MEDIA_UPLOADS)
        verifyNoInteractions(
            setBackupUseCase,
            sendMediaUploadsBackupHeartBeatUseCase
        )
    }

    @Test
    fun `test that backup is set when local media uploads is set`() = runTest {
        val handle = 123L
        val localFolder = "/path/to/media/upload"
        val backupName = "Media Uploads"
        whenever(getSecondarySyncHandleUseCase()).thenReturn(handle)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(localFolder)
        underTest(backupName)
        verify(setBackupUseCase).invoke(
            backupType = BackupInfoType.MEDIA_UPLOADS,
            targetNode = handle,
            localFolder = localFolder,
            backupName = backupName,
            state = BackupState.ACTIVE,
        )
        verify(broadcastBackupInfoTypeUseCase).invoke(BackupInfoType.MEDIA_UPLOADS)
        verify(sendMediaUploadsBackupHeartBeatUseCase).invoke(
            heartbeatStatus = HeartbeatStatus.UNKNOWN,
            lastNodeHandle = -1
        )
    }
}

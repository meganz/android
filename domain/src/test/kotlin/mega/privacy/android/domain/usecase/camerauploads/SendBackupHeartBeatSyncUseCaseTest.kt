package mega.privacy.android.domain.usecase.camerauploads

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFolderState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [SendBackupHeartBeatSyncUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendBackupHeartBeatSyncUseCaseTest {

    private lateinit var underTest: SendBackupHeartBeatSyncUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase = mock()

    private val fakeCameraUploadsState = CameraUploadsState(
        primaryCameraUploadsState = CameraUploadsFolderState(
            bytesToUploadCount = 69L,
        ),
        secondaryCameraUploadsState = CameraUploadsFolderState(
            bytesToUploadCount = 69L,
        ),
    )

    private val doNotSyncCameraUploadsState = CameraUploadsState(
        primaryCameraUploadsState = CameraUploadsFolderState(
            bytesToUploadCount = 0L,
        ),
        secondaryCameraUploadsState = CameraUploadsFolderState(
            bytesToUploadCount = 0L,
        ),
    )

    private val fakeBackup = Backup(
        backupId = 123L,
        backupInfoType = BackupInfoType.BACKUP_UPLOAD,
        targetNode = NodeId(123L),
        localFolder = "local",
        backupName = "camera uploads",
        state = BackupState.INVALID,
        subState = 1,
        extraData = "",
        targetFolderPath = "",
    )

    private val doNotSyncBackup = Backup(
        backupId = 123L,
        backupInfoType = BackupInfoType.BACKUP_UPLOAD,
        targetNode = NodeId(123L),
        localFolder = "local",
        backupName = "camera uploads",
        state = BackupState.PAUSE_UPLOADS,
        subState = 1,
        extraData = "",
        targetFolderPath = "",
    )

    @BeforeAll
    fun setUp() {
        underTest = SendBackupHeartBeatSyncUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            isMediaUploadsEnabledUseCase = isMediaUploadsEnabledUseCase
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            cameraUploadsRepository,
            isCameraUploadsEnabledUseCase,
            isMediaUploadsEnabledUseCase
        )
    }

    @Test
    internal fun `test that backup heartbeat sync is not sent when upload sync is not enabled`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            underTest { fakeCameraUploadsState }.test {
                verify(cameraUploadsRepository, times(0)).sendBackupHeartbeatSync(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that backup heartbeat sync is not sent when upload sync is enabled but no upload bytes exist`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
            whenever(cameraUploadsRepository.getCuBackUp()).thenReturn(fakeBackup)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(fakeBackup)
            underTest { doNotSyncCameraUploadsState }.test {
                verify(cameraUploadsRepository, times(0)).sendBackupHeartbeatSync(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that backup heartbeat sync is not sent when upload sync is enabled but backup state is PAUSE_UPLOADS`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
            whenever(cameraUploadsRepository.getCuBackUp()).thenReturn(doNotSyncBackup)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(doNotSyncBackup)
            underTest { fakeCameraUploadsState }.test {
                verify(cameraUploadsRepository, times(0)).sendBackupHeartbeatSync(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest(name = "number of heartbeats: {0}")
    @ValueSource(ints = [1, 2, 3, 10, 500])
    internal fun `test that backup heartbeat sync is sent when camera upload sync is enabled and upload bytes exist and backup state is not PAUSE_UPLOADS`(
        numberOfHeartbeats: Int,
    ) =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(false)
            whenever(cameraUploadsRepository.getCuBackUp()).thenReturn(fakeBackup)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(fakeBackup)
            underTest { fakeCameraUploadsState }.take(numberOfHeartbeats).collect()
            verify(cameraUploadsRepository, times(numberOfHeartbeats - 1)).sendBackupHeartbeatSync(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

    @ParameterizedTest(name = "number of heartbeats: {0}")
    @ValueSource(ints = [1, 2, 3, 10, 500])
    internal fun `test that backup heartbeat sync is sent when media upload sync is enabled and upload bytes exist and backup state is not PAUSE_UPLOADS`(
        numberOfHeartbeats: Int,
    ) =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
            whenever(cameraUploadsRepository.getCuBackUp()).thenReturn(fakeBackup)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(fakeBackup)
            underTest { fakeCameraUploadsState }.take(numberOfHeartbeats).collect()
            verify(cameraUploadsRepository, times(numberOfHeartbeats - 1)).sendBackupHeartbeatSync(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

    @ParameterizedTest(name = "number of heartbeats: {0}")
    @ValueSource(ints = [1, 2, 3, 10, 500])
    internal fun `test that backup heartbeat sync is sent for camera upload only if sync is enabled and upload bytes exist and backup state is not PAUSE_UPLOADS but backup is null for media uploads`(
        numberOfHeartbeats: Int,
    ) =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
            whenever(cameraUploadsRepository.getCuBackUp()).thenReturn(fakeBackup)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(null)
            underTest { fakeCameraUploadsState }.take(numberOfHeartbeats).collect()
            verify(cameraUploadsRepository, times(numberOfHeartbeats - 1)).sendBackupHeartbeatSync(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

    @ParameterizedTest(name = "number of heartbeats: {0}")
    @ValueSource(ints = [1, 2, 3, 10, 500])
    internal fun `test that backup heartbeat sync is sent for media upload only if sync is enabled and upload bytes exist and backup state is not PAUSE_UPLOADS but backup is null for camera uploads`(
        numberOfHeartbeats: Int,
    ) =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
            whenever(cameraUploadsRepository.getCuBackUp()).thenReturn(null)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(fakeBackup)
            underTest { fakeCameraUploadsState }.take(numberOfHeartbeats).collect()
            verify(cameraUploadsRepository, times(numberOfHeartbeats - 1)).sendBackupHeartbeatSync(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

    @ParameterizedTest(name = "number of heartbeats: {0}")
    @ValueSource(ints = [1, 2, 3, 10, 500])
    internal fun `test that backup heartbeat sync is sent when upload sync is enabled and upload bytes exist and backup state is not PAUSE_UPLOADS`(
        numberOfHeartbeats: Int,
    ) =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
            whenever(cameraUploadsRepository.getCuBackUp()).thenReturn(fakeBackup)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(fakeBackup)
            underTest { fakeCameraUploadsState }.take(numberOfHeartbeats).collect()
            verify(
                cameraUploadsRepository,
                times((numberOfHeartbeats - 1) * 2)
            ).sendBackupHeartbeatSync(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
}

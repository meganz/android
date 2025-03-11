package mega.privacy.android.feature.sync.data.mapper.sync

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.feature.sync.data.mapper.SyncStatusMapper
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncStats
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncStatusMapperTest {

    private val underTest = SyncStatusMapper()

    private val megaSyncStats: MegaSyncStats = mock()

    @Test
    fun `test that the sync status is paused if the run state is paused`() {
        val runState = MegaSync.SyncRunningState.RUNSTATE_SUSPENDED.swigValue()

        val syncStatus = underTest(syncStats = megaSyncStats, runningState = runState)

        assertThat(syncStatus).isEqualTo(SyncStatus.PAUSED)
    }

    @Test
    fun `test that the sync status is synced if sync stats are null and run state is running`() {
        val runState = MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue()

        val syncStatus = underTest(syncStats = megaSyncStats, runningState = runState)

        assertThat(syncStatus).isEqualTo(SyncStatus.SYNCED)
    }

    @Test
    fun `test that the sync status is syncing if sync stats are syncing and run state is running`() {
        val runState = MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue()
        whenever(megaSyncStats.isSyncing).thenReturn(true)
        whenever(megaSyncStats.isScanning).thenReturn(false)

        val syncStatus = underTest(syncStats = megaSyncStats, runningState = runState)

        assertThat(syncStatus).isEqualTo(SyncStatus.SYNCING)
    }

    @Test
    fun `test that the sync status is syncing if sync stats are scanning and run state is running`() {
        val runState = MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue()
        whenever(megaSyncStats.isSyncing).thenReturn(false)
        whenever(megaSyncStats.isScanning).thenReturn(true)

        val syncStatus = underTest(syncStats = megaSyncStats, runningState = runState)

        assertThat(syncStatus).isEqualTo(SyncStatus.SYNCING)
    }

    @ParameterizedTest(name = "when Backup state is {0} and Backup type is {1}, then Sync status is {2}")
    @MethodSource("provideBackupStateParameters")
    fun `test that Backup state mapping is correct`(
        backupState: BackupState,
        backupType: BackupInfoType,
        syncStatus: SyncStatus
    ) {
        assertThat(
            underTest(
                backupState = backupState,
                backupType = backupType,
            )
        ).isEqualTo(syncStatus)
    }

    @ParameterizedTest(name = "when Camera Uploads status is {0}, then Sync status is {1}")
    @MethodSource("provideCameraUploadsStatusParameters")
    fun `test that Camera Uploads status mapping is correct`(
        cuStatusInfo: CameraUploadsStatusInfo,
        syncStatus: SyncStatus
    ) {
        assertThat(
            underTest(
                backupState = BackupState.ACTIVE,
                backupType = BackupInfoType.CAMERA_UPLOADS,
                cuStatusInfo = cuStatusInfo,
            )
        ).isEqualTo(syncStatus)
    }

    @ParameterizedTest(name = "when Camera Uploads status is {0}, then SyncStatus is {1}")
    @MethodSource("provideMediaUploadsStatusParameters")
    fun `test that Media Uploads status mapping is correct`(
        cuStatusInfo: CameraUploadsStatusInfo,
        syncStatus: SyncStatus
    ) {
        assertThat(
            underTest(
                backupState = BackupState.ACTIVE,
                backupType = BackupInfoType.MEDIA_UPLOADS,
                cuStatusInfo = cuStatusInfo,
            )
        ).isEqualTo(syncStatus)
    }

    private fun provideBackupStateParameters() = Stream.of(
        Arguments.of(BackupState.INVALID, BackupInfoType.TWO_WAY_SYNC, SyncStatus.ERROR),
        Arguments.of(BackupState.INVALID, BackupInfoType.UP_SYNC, SyncStatus.ERROR),
        Arguments.of(BackupState.INVALID, BackupInfoType.CAMERA_UPLOADS, SyncStatus.ERROR),
        Arguments.of(BackupState.INVALID, BackupInfoType.MEDIA_UPLOADS, SyncStatus.ERROR),

        Arguments.of(BackupState.NOT_INITIALIZED, BackupInfoType.TWO_WAY_SYNC, SyncStatus.ERROR),
        Arguments.of(BackupState.NOT_INITIALIZED, BackupInfoType.UP_SYNC, SyncStatus.ERROR),
        Arguments.of(
            BackupState.NOT_INITIALIZED,
            BackupInfoType.CAMERA_UPLOADS,
            SyncStatus.DISABLED
        ),
        Arguments.of(
            BackupState.NOT_INITIALIZED,
            BackupInfoType.MEDIA_UPLOADS,
            SyncStatus.DISABLED
        ),

        Arguments.of(BackupState.ACTIVE, BackupInfoType.TWO_WAY_SYNC, SyncStatus.SYNCED),
        Arguments.of(BackupState.ACTIVE, BackupInfoType.UP_SYNC, SyncStatus.SYNCED),
        Arguments.of(BackupState.ACTIVE, BackupInfoType.CAMERA_UPLOADS, SyncStatus.SYNCED),
        Arguments.of(BackupState.ACTIVE, BackupInfoType.MEDIA_UPLOADS, SyncStatus.SYNCED),

        Arguments.of(BackupState.FAILED, BackupInfoType.TWO_WAY_SYNC, SyncStatus.ERROR),
        Arguments.of(BackupState.FAILED, BackupInfoType.UP_SYNC, SyncStatus.ERROR),
        Arguments.of(BackupState.FAILED, BackupInfoType.CAMERA_UPLOADS, SyncStatus.ERROR),
        Arguments.of(BackupState.FAILED, BackupInfoType.MEDIA_UPLOADS, SyncStatus.ERROR),

        Arguments.of(
            BackupState.TEMPORARILY_DISABLED,
            BackupInfoType.TWO_WAY_SYNC,
            SyncStatus.ERROR
        ),
        Arguments.of(BackupState.TEMPORARILY_DISABLED, BackupInfoType.UP_SYNC, SyncStatus.ERROR),
        Arguments.of(
            BackupState.TEMPORARILY_DISABLED,
            BackupInfoType.CAMERA_UPLOADS,
            SyncStatus.DISABLED
        ),
        Arguments.of(
            BackupState.TEMPORARILY_DISABLED,
            BackupInfoType.MEDIA_UPLOADS,
            SyncStatus.DISABLED
        ),

        Arguments.of(BackupState.DISABLED, BackupInfoType.TWO_WAY_SYNC, SyncStatus.ERROR),
        Arguments.of(BackupState.DISABLED, BackupInfoType.UP_SYNC, SyncStatus.ERROR),
        Arguments.of(BackupState.DISABLED, BackupInfoType.CAMERA_UPLOADS, SyncStatus.DISABLED),
        Arguments.of(BackupState.DISABLED, BackupInfoType.MEDIA_UPLOADS, SyncStatus.DISABLED),

        Arguments.of(BackupState.PAUSE_UPLOADS, BackupInfoType.TWO_WAY_SYNC, SyncStatus.PAUSED),
        Arguments.of(BackupState.PAUSE_UPLOADS, BackupInfoType.UP_SYNC, SyncStatus.PAUSED),
        Arguments.of(BackupState.PAUSE_UPLOADS, BackupInfoType.CAMERA_UPLOADS, SyncStatus.DISABLED),
        Arguments.of(BackupState.PAUSE_UPLOADS, BackupInfoType.MEDIA_UPLOADS, SyncStatus.DISABLED),

        Arguments.of(BackupState.PAUSE_DOWNLOADS, BackupInfoType.TWO_WAY_SYNC, SyncStatus.PAUSED),
        Arguments.of(BackupState.PAUSE_DOWNLOADS, BackupInfoType.UP_SYNC, SyncStatus.PAUSED),
        Arguments.of(
            BackupState.PAUSE_DOWNLOADS,
            BackupInfoType.CAMERA_UPLOADS,
            SyncStatus.DISABLED
        ),
        Arguments.of(
            BackupState.PAUSE_DOWNLOADS,
            BackupInfoType.MEDIA_UPLOADS,
            SyncStatus.DISABLED
        ),

        Arguments.of(BackupState.PAUSE_ALL, BackupInfoType.TWO_WAY_SYNC, SyncStatus.PAUSED),
        Arguments.of(BackupState.PAUSE_ALL, BackupInfoType.UP_SYNC, SyncStatus.PAUSED),
        Arguments.of(BackupState.PAUSE_ALL, BackupInfoType.CAMERA_UPLOADS, SyncStatus.DISABLED),
        Arguments.of(BackupState.PAUSE_ALL, BackupInfoType.MEDIA_UPLOADS, SyncStatus.DISABLED),

        Arguments.of(BackupState.DELETED, BackupInfoType.TWO_WAY_SYNC, SyncStatus.ERROR),
        Arguments.of(BackupState.DELETED, BackupInfoType.UP_SYNC, SyncStatus.ERROR),
        Arguments.of(BackupState.DELETED, BackupInfoType.CAMERA_UPLOADS, SyncStatus.ERROR),
        Arguments.of(BackupState.DELETED, BackupInfoType.MEDIA_UPLOADS, SyncStatus.ERROR),
    )

    private fun provideCameraUploadsStatusParameters() = Stream.of(
        Arguments.of(CameraUploadsStatusInfo.Unknown, SyncStatus.SYNCED),

        Arguments.of(CameraUploadsStatusInfo.Started, SyncStatus.SYNCING),
        Arguments.of(CameraUploadsStatusInfo.CheckFilesForUpload, SyncStatus.SYNCING),
        Arguments.of(mock<CameraUploadsStatusInfo.UploadProgress>(), SyncStatus.SYNCING),
        Arguments.of(mock<CameraUploadsStatusInfo.VideoCompressionProgress>(), SyncStatus.SYNCING),
        Arguments.of(CameraUploadsStatusInfo.VideoCompressionSuccess, SyncStatus.SYNCING),

        Arguments.of(CameraUploadsStatusInfo.VideoCompressionOutOfSpace, SyncStatus.ERROR),
        Arguments.of(CameraUploadsStatusInfo.VideoCompressionError, SyncStatus.ERROR),
        Arguments.of(CameraUploadsStatusInfo.StorageOverQuota, SyncStatus.ERROR),
        Arguments.of(CameraUploadsStatusInfo.NotEnoughStorage, SyncStatus.ERROR),


        Arguments.of(
            CameraUploadsStatusInfo.FolderUnavailable(CameraUploadFolderType.Primary),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.FolderUnavailable(CameraUploadFolderType.Secondary),
            SyncStatus.DISABLED
        ),

        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.DISABLED),
            SyncStatus.DISABLED
        ),

        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.LOGIN_FAILED),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.LOCAL_PRIMARY_FOLDER_NOT_VALID),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.MEDIA_PERMISSION_NOT_GRANTED),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.TARGET_NODES_DELETED),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.INSUFFICIENT_LOCAL_STORAGE_SPACE),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.ERROR_DURING_PROCESS),
            SyncStatus.ERROR
        ),

        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW),
            SyncStatus.DISABLED
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET),
            SyncStatus.DISABLED
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA),
            SyncStatus.DISABLED
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET),
            SyncStatus.DISABLED
        ),

        Arguments.of(mock<CameraUploadsStatusInfo.Finished>(), SyncStatus.SYNCED),
    )

    private fun provideMediaUploadsStatusParameters() = Stream.of(
        Arguments.of(CameraUploadsStatusInfo.Unknown, SyncStatus.SYNCED),

        Arguments.of(CameraUploadsStatusInfo.Started, SyncStatus.SYNCING),
        Arguments.of(CameraUploadsStatusInfo.CheckFilesForUpload, SyncStatus.SYNCING),
        Arguments.of(mock<CameraUploadsStatusInfo.UploadProgress>(), SyncStatus.SYNCING),
        Arguments.of(mock<CameraUploadsStatusInfo.VideoCompressionProgress>(), SyncStatus.SYNCING),
        Arguments.of(CameraUploadsStatusInfo.VideoCompressionSuccess, SyncStatus.SYNCING),

        Arguments.of(CameraUploadsStatusInfo.VideoCompressionOutOfSpace, SyncStatus.ERROR),
        Arguments.of(CameraUploadsStatusInfo.VideoCompressionError, SyncStatus.ERROR),
        Arguments.of(CameraUploadsStatusInfo.StorageOverQuota, SyncStatus.ERROR),
        Arguments.of(CameraUploadsStatusInfo.NotEnoughStorage, SyncStatus.ERROR),


        Arguments.of(
            CameraUploadsStatusInfo.FolderUnavailable(CameraUploadFolderType.Primary),
            SyncStatus.DISABLED
        ),
        Arguments.of(
            CameraUploadsStatusInfo.FolderUnavailable(CameraUploadFolderType.Secondary),
            SyncStatus.ERROR
        ),

        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.DISABLED),
            SyncStatus.DISABLED
        ),

        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.LOGIN_FAILED),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.LOCAL_PRIMARY_FOLDER_NOT_VALID),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.MEDIA_PERMISSION_NOT_GRANTED),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.TARGET_NODES_DELETED),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.INSUFFICIENT_LOCAL_STORAGE_SPACE),
            SyncStatus.ERROR
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.ERROR_DURING_PROCESS),
            SyncStatus.ERROR
        ),

        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW),
            SyncStatus.DISABLED
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET),
            SyncStatus.DISABLED
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA),
            SyncStatus.DISABLED
        ),
        Arguments.of(
            CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET),
            SyncStatus.DISABLED
        ),

        Arguments.of(mock<CameraUploadsStatusInfo.Finished>(), SyncStatus.SYNCED),
    )
}
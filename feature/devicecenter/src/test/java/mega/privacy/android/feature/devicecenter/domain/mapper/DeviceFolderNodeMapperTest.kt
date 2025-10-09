package mega.privacy.android.feature.devicecenter.domain.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoHeartbeatStatus
import mega.privacy.android.domain.entity.backup.BackupInfoState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderStatus
import mega.privacy.android.feature.devicecenter.domain.usecase.mapper.DeviceFolderNodeMapper
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import java.util.concurrent.TimeUnit

/**
 * Test class for [DeviceFolderNodeMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceFolderNodeMapperTest {
    private lateinit var underTest: DeviceFolderNodeMapper

    private val currentTimeInSeconds = System.currentTimeMillis() / 1000L

    @BeforeAll
    fun setUp() {
        underTest = DeviceFolderNodeMapper()
    }

    @Test
    fun `test that the mapped device folder has an error status`() {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { userAgent }.thenReturn(backupUserAgent)
                on { state }.thenReturn(BackupInfoState.NOT_INITIALIZED)
                on { type }.thenReturn(backupType)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { localFolderPath }.thenReturn(localPath)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Error(null),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when backup state is {0}")
    @EnumSource(value = BackupInfoState::class, names = ["FAILED", "TEMPORARY_DISABLED"])
    fun `test that the mapped device folder has an error status if account is overquota`(backupState: BackupInfoState) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupSubState = SyncError.STORAGE_OVERQUOTA
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(backupState)
                on { subState }.thenReturn(SyncError.STORAGE_OVERQUOTA)
                on { type }.thenReturn(backupType)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Error(backupSubState),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "and backup sub state is {0}")
    @EnumSource(
        value = SyncError::class,
        names = ["ACCOUNT_EXPIRED", "ACCOUNT_BLOCKED", "NO_SYNC_ERROR", "STORAGE_OVERQUOTA"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the mapped device folder has an error status when the backup state is failed`(
        backupSubState: SyncError,
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(BackupInfoState.FAILED)
                on { subState }.thenReturn(backupSubState)
                on { type }.thenReturn(backupType)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Error(backupSubState),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "and backup sub state is {0}")
    @EnumSource(
        value = SyncError::class,
        names = ["ACCOUNT_EXPIRED", "ACCOUNT_BLOCKED", "NO_SYNC_ERROR", "STORAGE_OVERQUOTA"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the mapped device folder has an error status when the backup state is temporary disabled`(
        backupSubState: SyncError,
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(BackupInfoState.TEMPORARY_DISABLED)
                on { subState }.thenReturn(backupSubState)
                on { type }.thenReturn(backupType)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Error(backupSubState),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has an error status when has stalled issues`() {
        val backupId = 123456L
        val backupName = "Some Sync"
        val backupType = BackupInfoType.TWO_WAY_SYNC
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { userAgent }.thenReturn(backupUserAgent)
                on { state }.thenReturn(BackupInfoState.ACTIVE)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.STALLED)
                on { type }.thenReturn(backupType)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { localFolderPath }.thenReturn(localPath)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Error(null),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has a disabled status`() {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(BackupInfoState.DISABLED)
                on { type }.thenReturn(backupType)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Disabled,
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "backup type: {0}")
    @EnumSource(
        value = BackupInfoType::class,
        names = ["CAMERA_UPLOADS", "MEDIA_UPLOADS"]
    )
    fun `test that the mapped mobile device folder has an error status if it is offline (beyond the maximum created backup time)`(
        backupType: BackupInfoType,
    ) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTimeInSeconds.minus(TimeUnit.HOURS.toSeconds(2)))
                on { lastActivityTimestamp }.thenReturn(0L)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Error(null),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "backup type: {0}")
    @EnumSource(
        value = BackupInfoType::class,
        names = ["CAMERA_UPLOADS", "MEDIA_UPLOADS"]
    )
    fun `test that the mapped mobile device folder has an error status if it has an invalid handle`(
        backupType: BackupInfoType,
    ) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTimeInSeconds.minus(TimeUnit.HOURS.toSeconds(2)))
                on { lastActivityTimestamp }.thenReturn(0L)
                on { rootHandle }.thenReturn(NodeId(MegaApiJava.INVALID_HANDLE))
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Error(null),
                rootHandle = NodeId(MegaApiJava.INVALID_HANDLE),
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "backup type: {0}")
    @EnumSource(
        value = BackupInfoType::class,
        names = ["CAMERA_UPLOADS", "MEDIA_UPLOADS"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the mapped non-mobile device folder has an error status if it is beyond the maximum created backup time`(
        backupType: BackupInfoType,
    ) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupUserAgent = BackupInfoUserAgent.WINDOWS
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTimeInSeconds.minus(TimeUnit.HOURS.toSeconds(1)))
                on { lastActivityTimestamp }.thenReturn(0L)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Error(null),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "backup type: {0}")
    @EnumSource(
        value = BackupInfoType::class,
        names = ["CAMERA_UPLOADS", "MEDIA_UPLOADS"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the mapped non-mobile device folder has an error status if it is has an invalid handle`(
        backupType: BackupInfoType,
    ) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupUserAgent = BackupInfoUserAgent.WINDOWS
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTimeInSeconds.minus(TimeUnit.HOURS.toSeconds(1)))
                on { lastActivityTimestamp }.thenReturn(0L)
                on { rootHandle }.thenReturn(NodeId(MegaApiJava.INVALID_HANDLE))
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Error(null),
                rootHandle = NodeId(MegaApiJava.INVALID_HANDLE),
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_UP", "PAUSE_DOWN", "PAUSE_FULL", "DELETED"]
    )
    fun `test that the mapped device folder has a paused status when the backup type is a two way sync`(
        backupState: BackupInfoState,
    ) = testPausedOrDisabledStatus(
        backupType = BackupInfoType.TWO_WAY_SYNC,
        backupState = backupState,
    )

    @ParameterizedTest(name = " and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_UP", "PAUSE_FULL"]
    )
    fun `test that the mapped device folder has a paused status when the backup type is an upload sync`(
        backupState: BackupInfoState,
    ) = testPausedOrDisabledStatus(
        backupType = BackupInfoType.UP_SYNC,
        backupState = backupState,
    )

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_UP", "PAUSE_FULL"]
    )
    fun `test that the mapped device folder has a disabled status when the backup type is camera uploads`(
        backupState: BackupInfoState,
    ) = testPausedOrDisabledStatus(
        backupType = BackupInfoType.CAMERA_UPLOADS,
        backupState = backupState,
    )

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_UP", "PAUSE_FULL"]
    )
    fun `test that the mapped device folder has a disabled status when the backup type is media uploads`(
        backupState: BackupInfoState,
    ) = testPausedOrDisabledStatus(
        backupType = BackupInfoType.MEDIA_UPLOADS,
        backupState = backupState,
    )

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_UP", "PAUSE_FULL"]
    )
    fun `test that the mapped device folder has a paused status when the backup type is a backup upload`(
        backupState: BackupInfoState,
    ) = testPausedOrDisabledStatus(
        backupType = BackupInfoType.BACKUP_UPLOAD,
        backupState = backupState,
    )

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_DOWN", "PAUSE_FULL", "DELETED"]
    )
    fun `test that the mapped device folder has a paused status when the backup type is a download sync`(
        backupState: BackupInfoState,
    ) = testPausedOrDisabledStatus(
        backupType = BackupInfoType.DOWN_SYNC,
        backupState = backupState,
    )

    private fun testPausedOrDisabledStatus(
        backupType: BackupInfoType,
        backupState: BackupInfoState,
    ) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupUserAgent = BackupInfoUserAgent.UNKNOWN
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(backupType)
                on { state }.thenReturn(backupState)
                on { timestamp }.thenReturn(currentTimeInSeconds)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
                on { userAgent }.thenReturn(backupUserAgent)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = when (backupType) {
                    BackupInfoType.CAMERA_UPLOADS, BackupInfoType.MEDIA_UPLOADS -> DeviceFolderStatus.Disabled
                    else -> DeviceFolderStatus.Paused
                },
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["ACTIVE", "PAUSE_UP", "PAUSE_DOWN", "PAUSE_FULL", "DELETED"],
    )
    fun `test that the mapped device folder has an up to date status when the heartbeat status is up to date`(
        backupState: BackupInfoState,
    ) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.INVALID
        val backupUserAgent = BackupInfoUserAgent.WINDOWS
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(backupState)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.UPTODATE)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTimeInSeconds)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
                on { userAgent }.thenReturn(backupUserAgent)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.UpToDate,
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["ACTIVE", "PAUSE_UP", "PAUSE_DOWN", "PAUSE_FULL", "DELETED"],
    )
    fun `test that the mapped device folder has an up to date status when the heartbeat status is inactive`(
        backupState: BackupInfoState,
    ) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.INVALID
        val backupUserAgent = BackupInfoUserAgent.WINDOWS
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(backupState)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.INACTIVE)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTimeInSeconds)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
                on { userAgent }.thenReturn(backupUserAgent)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.UpToDate,
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has an updating status when the heartbeat status is unknown`() {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.UNKNOWN)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTimeInSeconds)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
                on { userAgent }.thenReturn(backupUserAgent)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Updating(0),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has an updating status the heartbeat status is syncing`() {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupProgress = 75
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.SYNCING)
                on { progress }.thenReturn(backupProgress)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTimeInSeconds)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
                on { userAgent }.thenReturn(backupUserAgent)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Updating(backupProgress),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has an updating status the heartbeat status is pending`() {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.WINDOWS
        val backupRootHandle = NodeId(789012L)
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.PENDING)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTimeInSeconds)
                on { lastActivityTimestamp }.thenReturn(currentTimeInSeconds)
                on { userAgent }.thenReturn(backupUserAgent)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceFolderStatus.Updating(0),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }
}

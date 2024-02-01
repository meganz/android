package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoHeartbeatStatus
import mega.privacy.android.domain.entity.backup.BackupInfoState
import mega.privacy.android.domain.entity.backup.BackupInfoSubState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
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

    @BeforeAll
    fun setUp() {
        underTest = DeviceFolderNodeMapper()
    }

    @Test
    fun `test that the mapped device folder has a stopped status`() {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = 789012L
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
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Stopped,
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
    fun `test that the mapped device folder has an overquota status`(backupState: BackupInfoState) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupSubState = BackupInfoSubState.STORAGE_OVERQUOTA
        val backupRootHandle = 789012L
        val localPath = "storage/emulated/0/DCIM/Camera"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(backupState)
                on { subState }.thenReturn(BackupInfoSubState.STORAGE_OVERQUOTA)
                on { type }.thenReturn(backupType)
                on { rootHandle }.thenReturn(backupRootHandle)
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Overquota(backupSubState),
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
        value = BackupInfoSubState::class, names = ["STORAGE_OVERQUOTA"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the mapped device folder has a blocked status when the backup state is failed`(
        backupSubState: BackupInfoSubState,
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = 789012L
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
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Blocked(backupSubState),
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
        value = BackupInfoSubState::class, names = ["STORAGE_OVERQUOTA"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the mapped device folder has a blocked status when the backup state is temporary disabled`(
        backupSubState: BackupInfoSubState,
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = 789012L
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
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Blocked(backupSubState),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has a stalled status`() {
        val backupId = 123456L
        val backupName = "Some Sync"
        val backupType = BackupInfoType.TWO_WAY_SYNC
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = 789012L
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
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Stalled,
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
        val backupRootHandle = 789012L
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
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Disabled,
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
    fun `test that the mapped mobile device folder has an offline status if it is beyond the maximum created backup time`(
        backupType: BackupInfoType,
    ) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = 789012L
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
                status = DeviceCenterNodeStatus.Offline,
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
    fun `test that the mapped mobile device folder has an offline status if it has an invalid handle`(
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
                on { rootHandle }.thenReturn(MegaApiJava.INVALID_HANDLE)
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Offline,
                rootHandle = MegaApiJava.INVALID_HANDLE,
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
    fun `test that the mapped non-mobile device folder has an offline status if it is beyond the maximum created backup time`(
        backupType: BackupInfoType,
    ) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupUserAgent = BackupInfoUserAgent.WINDOWS
        val backupRootHandle = 789012L
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
                status = DeviceCenterNodeStatus.Offline,
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
    fun `test that the mapped non-mobile device folder has an offline status if it is has an invalid handle`(
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
                on { rootHandle }.thenReturn(MegaApiJava.INVALID_HANDLE)
                on { userAgent }.thenReturn(backupUserAgent)
                on { localFolderPath }.thenReturn(localPath)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Offline,
                rootHandle = MegaApiJava.INVALID_HANDLE,
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
    ) = testPausedStatus(
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
    ) = testPausedStatus(
        backupType = BackupInfoType.UP_SYNC,
        backupState = backupState,
    )

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_UP", "PAUSE_FULL"]
    )
    fun `test that the mapped device folder has a paused status when the backup type is camera uploads`(
        backupState: BackupInfoState,
    ) = testPausedStatus(
        backupType = BackupInfoType.CAMERA_UPLOADS,
        backupState = backupState,
    )

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_UP", "PAUSE_FULL"]
    )
    fun `test that the mapped device folder has a paused status when the backup type is media uploads`(
        backupState: BackupInfoState,
    ) = testPausedStatus(
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
    ) = testPausedStatus(
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
    ) = testPausedStatus(
        backupType = BackupInfoType.DOWN_SYNC,
        backupState = backupState,
    )

    private fun testPausedStatus(backupType: BackupInfoType, backupState: BackupInfoState) {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupUserAgent = BackupInfoUserAgent.UNKNOWN
        val backupRootHandle = 789012L
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
                status = DeviceCenterNodeStatus.Paused,
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
        val backupRootHandle = 789012L
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
                status = DeviceCenterNodeStatus.UpToDate,
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
        val backupRootHandle = 789012L
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
                status = DeviceCenterNodeStatus.UpToDate,
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has an initializing status`() {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupRootHandle = 789012L
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
                status = DeviceCenterNodeStatus.Initializing,
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has a syncing status`() {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.ANDROID
        val backupProgress = 75
        val backupRootHandle = 789012L
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
                status = DeviceCenterNodeStatus.Syncing(backupProgress),
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has a scanning status`() {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        val backupId = 123456L
        val backupName = "Backup One"
        val backupType = BackupInfoType.CAMERA_UPLOADS
        val backupUserAgent = BackupInfoUserAgent.WINDOWS
        val backupRootHandle = 789012L
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
                status = DeviceCenterNodeStatus.Scanning,
                rootHandle = backupRootHandle,
                type = backupType,
                userAgent = backupUserAgent,
                localFolderPath = localPath
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }
}
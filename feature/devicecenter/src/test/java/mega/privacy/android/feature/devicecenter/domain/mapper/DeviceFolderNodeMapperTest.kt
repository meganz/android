package mega.privacy.android.feature.devicecenter.domain.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfo
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoHeartbeatStatus
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoState
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoSubState
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
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
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(BackupInfoState.NOT_INITIALIZED)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Stopped,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when backup state is {0}")
    @EnumSource(value = BackupInfoState::class, names = ["FAILED", "TEMPORARY_DISABLED"])
    fun `test that the mapped device folder has an overquota status`(backupState: BackupInfoState) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(backupState)
                on { subState }.thenReturn(BackupInfoSubState.STORAGE_OVERQUOTA)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Overquota,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "and backup sub state is {0}")
    @EnumSource(
        value = BackupInfoSubState::class, names = ["STORAGE_OVERQUOTA"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the mapped device folder has a blocked status when the backup statue is failed`(
        backupSubState: BackupInfoSubState,
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(BackupInfoState.FAILED)
                on { subState }.thenReturn(backupSubState)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Blocked(backupSubState),
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "and backup sub state is {0}")
    @EnumSource(
        value = BackupInfoSubState::class, names = ["STORAGE_OVERQUOTA"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the mapped device folder has a blocked status when the backup statue is temporary disabled`(
        backupSubState: BackupInfoSubState,
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(BackupInfoState.TEMPORARY_DISABLED)
                on { subState }.thenReturn(backupSubState)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Blocked(backupSubState),
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has a disabled status`() {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(BackupInfoState.DISABLED)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Disabled,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "mobile backup type: {0}")
    @EnumSource(
        value = BackupInfoType::class,
        names = ["CAMERA_UPLOADS", "MEDIA_UPLOADS"]
    )
    fun `test that the mapped device folder has an offline status when the backup is a mobile backup`(
        backupType: BackupInfoType,
    ) {
        val currentTime = System.currentTimeMillis()
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTime.minus(TimeUnit.HOURS.toMillis(2)))
                on { lastActivityTimestamp }.thenReturn(TimeUnit.HOURS.toMillis(2))
                on { rootHandle }.thenReturn(1000L)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Offline,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "non mobile backup type: {0}")
    @EnumSource(
        value = BackupInfoType::class,
        names = ["CAMERA_UPLOADS", "MEDIA_UPLOADS"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the mapped device folder has an offline status when the backup is a non mobile backup`(
        backupType: BackupInfoType,
    ) {
        val currentTime = System.currentTimeMillis()
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(backupType)
                on { timestamp }.thenReturn(currentTime.minus(7200000L))
                on { lastActivityTimestamp }.thenReturn(currentTime.minus(7200000L))
                on { rootHandle }.thenReturn(1000L)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Offline,
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
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(BackupInfoType.TWO_WAY_SYNC)
                on { state }.thenReturn(backupState)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Paused,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_UP", "PAUSE_FULL"]
    )
    fun `test that the mapped device folder has a paused status when the backup type is camera uploads`(
        backupState: BackupInfoState,
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(BackupInfoType.CAMERA_UPLOADS)
                on { state }.thenReturn(backupState)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Paused,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_UP", "PAUSE_FULL"]
    )
    fun `test that the mapped device folder has a paused status when the backup type is media uploads`(
        backupState: BackupInfoState,
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(BackupInfoType.MEDIA_UPLOADS)
                on { state }.thenReturn(backupState)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Paused,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_DOWN", "PAUSE_FULL", "DELETED"]
    )
    fun `test that the mapped device folder has a paused status when the backup type is a download sync`(
        backupState: BackupInfoState,
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(BackupInfoType.DOWN_SYNC)
                on { state }.thenReturn(backupState)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Paused,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @ParameterizedTest(name = " and backup state is {0}")
    @EnumSource(
        value = BackupInfoState::class,
        names = ["PAUSE_UP", "PAUSE_FULL"]
    )
    fun `test that the mapped device folder has a paused status when the backup type is an upload sync`(
        backupState: BackupInfoState,
    ) {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { type }.thenReturn(BackupInfoType.CAMERA_UPLOADS)
                on { state }.thenReturn(backupState)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Paused,
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
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(backupState)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.UPTODATE)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.UpToDate,
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
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { state }.thenReturn(backupState)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.INACTIVE)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.UpToDate,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has an initializing status`() {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.UNKNOWN)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Initializing,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has a syncing status`() {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupProgress = 75
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.SYNCING)
                on { progress }.thenReturn(backupProgress)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Syncing(backupProgress),
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }

    @Test
    fun `test that the mapped device folder has a scanning status`() {
        val backupId = 123456L
        val backupName = "Backup One"
        val backupInfoList = listOf<BackupInfo>(
            mock {
                on { id }.thenReturn(backupId)
                on { name }.thenReturn(backupName)
                on { status }.thenReturn(BackupInfoHeartbeatStatus.PENDING)
            },
        )
        val expected = listOf(
            DeviceFolderNode(
                id = backupId.toString(),
                name = backupName,
                status = DeviceCenterNodeStatus.Scanning,
            )
        )
        assertThat(underTest(backupInfoList)).isEqualTo(expected)
    }
}
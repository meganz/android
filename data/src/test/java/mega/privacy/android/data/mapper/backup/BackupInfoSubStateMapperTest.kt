package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfoSubState
import nz.mega.sdk.MegaSync.Error
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [BackupInfoSubStateMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupInfoSubStateMapperTest {
    private lateinit var underTest: BackupInfoSubStateMapper

    @BeforeAll
    fun setUp() {
        underTest = BackupInfoSubStateMapper()
    }

    @ParameterizedTest(name = "when sdkSubState is {0}, then backupInfoSubState is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(
        sdkSubState: Int,
        backupInfoSubState: BackupInfoSubState,
    ) {
        assertThat(underTest(sdkSubState)).isEqualTo(backupInfoSubState)
    }

    @Test
    fun `test that a non matching value returns a default sub state`() {
        assertThat(underTest(-100)).isEqualTo(BackupInfoSubState.UNKNOWN_BACKUP_INFO_SUB_STATE)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(Error.NO_SYNC_ERROR.swigValue(), BackupInfoSubState.NO_SYNC_ERROR),
        Arguments.of(Error.UNKNOWN_ERROR.swigValue(), BackupInfoSubState.UNKNOWN_ERROR),
        Arguments.of(
            Error.UNSUPPORTED_FILE_SYSTEM.swigValue(),
            BackupInfoSubState.UNSUPPORTED_FILE_SYSTEM,
        ),
        Arguments.of(Error.INVALID_REMOTE_TYPE.swigValue(), BackupInfoSubState.INVALID_REMOTE_TYPE),
        Arguments.of(Error.INVALID_LOCAL_TYPE.swigValue(), BackupInfoSubState.INVALID_LOCAL_TYPE),
        Arguments.of(Error.INITIAL_SCAN_FAILED.swigValue(), BackupInfoSubState.INITIAL_SCAN_FAILED),
        Arguments.of(
            Error.LOCAL_PATH_TEMPORARY_UNAVAILABLE.swigValue(),
            BackupInfoSubState.LOCAL_PATH_TEMPORARY_UNAVAILABLE,
        ),
        Arguments.of(
            Error.LOCAL_PATH_UNAVAILABLE.swigValue(),
            BackupInfoSubState.LOCAL_PATH_UNAVAILABLE,
        ),
        Arguments.of(
            Error.REMOTE_NODE_NOT_FOUND.swigValue(),
            BackupInfoSubState.REMOTE_NODE_NOT_FOUND,
        ),
        Arguments.of(Error.STORAGE_OVERQUOTA.swigValue(), BackupInfoSubState.STORAGE_OVERQUOTA),
        Arguments.of(Error.ACCOUNT_EXPIRED.swigValue(), BackupInfoSubState.ACCOUNT_EXPIRED),
        Arguments.of(
            Error.FOREIGN_TARGET_OVERSTORAGE.swigValue(),
            BackupInfoSubState.FOREIGN_TARGET_OVERSTORAGE,
        ),
        Arguments.of(
            Error.REMOTE_PATH_HAS_CHANGED.swigValue(),
            BackupInfoSubState.REMOTE_PATH_HAS_CHANGED,
        ),
        Arguments.of(
            Error.SHARE_NON_FULL_ACCESS.swigValue(),
            BackupInfoSubState.SHARE_NON_FULL_ACCESS,
        ),
        Arguments.of(
            Error.LOCAL_FILESYSTEM_MISMATCH.swigValue(),
            BackupInfoSubState.LOCAL_FILESYSTEM_MISMATCH,
        ),
        Arguments.of(Error.PUT_NODES_ERROR.swigValue(), BackupInfoSubState.PUT_NODES_ERROR),
        Arguments.of(
            Error.ACTIVE_SYNC_BELOW_PATH.swigValue(),
            BackupInfoSubState.ACTIVE_SYNC_BELOW_PATH,
        ),
        Arguments.of(
            Error.ACTIVE_SYNC_ABOVE_PATH.swigValue(),
            BackupInfoSubState.ACTIVE_SYNC_ABOVE_PATH,
        ),
        Arguments.of(
            Error.REMOTE_NODE_MOVED_TO_RUBBISH.swigValue(),
            BackupInfoSubState.REMOTE_NODE_MOVED_TO_RUBBISH,
        ),
        Arguments.of(
            Error.REMOTE_NODE_INSIDE_RUBBISH.swigValue(),
            BackupInfoSubState.REMOTE_NODE_INSIDE_RUBBISH,
        ),
        Arguments.of(
            Error.VBOXSHAREDFOLDER_UNSUPPORTED.swigValue(),
            BackupInfoSubState.VBOXSHAREDFOLDER_UNSUPPORTED,
        ),
        Arguments.of(
            Error.LOCAL_PATH_SYNC_COLLISION.swigValue(),
            BackupInfoSubState.LOCAL_PATH_SYNC_COLLISION,
        ),
        Arguments.of(Error.ACCOUNT_BLOCKED.swigValue(), BackupInfoSubState.ACCOUNT_BLOCKED),
        Arguments.of(
            Error.UNKNOWN_TEMPORARY_ERROR.swigValue(),
            BackupInfoSubState.UNKNOWN_TEMPORARY_ERROR,
        ),
        Arguments.of(
            Error.TOO_MANY_ACTION_PACKETS.swigValue(),
            BackupInfoSubState.TOO_MANY_ACTION_PACKETS,
        ),
        Arguments.of(Error.LOGGED_OUT.swigValue(), BackupInfoSubState.LOGGED_OUT),
        Arguments.of(Error.MISSING_PARENT_NODE.swigValue(), BackupInfoSubState.MISSING_PARENT_NODE),
        Arguments.of(Error.BACKUP_MODIFIED.swigValue(), BackupInfoSubState.BACKUP_MODIFIED),
        Arguments.of(
            Error.BACKUP_SOURCE_NOT_BELOW_DRIVE.swigValue(),
            BackupInfoSubState.BACKUP_SOURCE_NOT_BELOW_DRIVE,
        ),
        Arguments.of(
            Error.SYNC_CONFIG_WRITE_FAILURE.swigValue(),
            BackupInfoSubState.SYNC_CONFIG_WRITE_FAILURE,
        ),
        Arguments.of(
            Error.ACTIVE_SYNC_SAME_PATH.swigValue(),
            BackupInfoSubState.ACTIVE_SYNC_SAME_PATH,
        ),
        Arguments.of(
            Error.COULD_NOT_MOVE_CLOUD_NODES.swigValue(),
            BackupInfoSubState.COULD_NOT_MOVE_CLOUD_NODES,
        ),
        Arguments.of(
            Error.COULD_NOT_CREATE_IGNORE_FILE.swigValue(),
            BackupInfoSubState.COULD_NOT_CREATE_IGNORE_FILE,
        ),
        Arguments.of(
            Error.SYNC_CONFIG_READ_FAILURE.swigValue(),
            BackupInfoSubState.SYNC_CONFIG_READ_FAILURE,
        ),
        Arguments.of(Error.UNKNOWN_DRIVE_PATH.swigValue(), BackupInfoSubState.UNKNOWN_DRIVE_PATH),
        Arguments.of(
            Error.INVALID_SCAN_INTERVAL.swigValue(),
            BackupInfoSubState.INVALID_SCAN_INTERVAL,
        ),
        Arguments.of(
            Error.NOTIFICATION_SYSTEM_UNAVAILABLE.swigValue(),
            BackupInfoSubState.NOTIFICATION_SYSTEM_UNAVAILABLE,
        ),
        Arguments.of(Error.UNABLE_TO_ADD_WATCH.swigValue(), BackupInfoSubState.UNABLE_TO_ADD_WATCH),
        Arguments.of(
            Error.UNABLE_TO_RETRIEVE_ROOT_FSID.swigValue(),
            BackupInfoSubState.UNABLE_TO_RETRIEVE_ROOT_FSID,
        ),
        Arguments.of(
            Error.UNABLE_TO_OPEN_DATABASE.swigValue(),
            BackupInfoSubState.UNABLE_TO_OPEN_DATABASE,
        ),
        Arguments.of(
            Error.INSUFFICIENT_DISK_SPACE.swigValue(),
            BackupInfoSubState.INSUFFICIENT_DISK_SPACE,
        ),
        Arguments.of(
            Error.FAILURE_ACCESSING_PERSISTENT_STORAGE.swigValue(),
            BackupInfoSubState.FAILURE_ACCESSING_PERSISTENT_STORAGE,
        ),
        Arguments.of(
            Error.MISMATCH_OF_ROOT_FSID.swigValue(),
            BackupInfoSubState.MISMATCH_OF_ROOT_FSID,
        ),
        Arguments.of(
            Error.FILESYSTEM_FILE_IDS_ARE_UNSTABLE.swigValue(),
            BackupInfoSubState.FILESYSTEM_FILE_IDS_ARE_UNSTABLE,
        ),
        Arguments.of(
            Error.FILESYSTEM_ID_UNAVAILABLE.swigValue(),
            BackupInfoSubState.FILESYSTEM_ID_UNAVAILABLE,
        ),
    )
}
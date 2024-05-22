package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.sync.SyncError
import nz.mega.sdk.MegaSync.Error
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [SyncErrorMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncErrorMapperTest {
    private lateinit var underTest: SyncErrorMapper

    @BeforeAll
    fun setUp() {
        underTest = SyncErrorMapper()
    }

    @ParameterizedTest(name = "when sdkSubState is {0}, then backupInfoSubState is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(
        sdkSubState: Int,
        syncError: SyncError,
    ) {
        assertThat(underTest(sdkSubState)).isEqualTo(syncError)
    }

    @Test
    fun `test that a non matching value returns a default sub state`() {
        assertThat(underTest(-100)).isEqualTo(SyncError.UNKNOWN_BACKUP_INFO_SUB_STATE)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(Error.NO_SYNC_ERROR.swigValue(), SyncError.NO_SYNC_ERROR),
        Arguments.of(Error.UNKNOWN_ERROR.swigValue(), SyncError.UNKNOWN_ERROR),
        Arguments.of(
            Error.UNSUPPORTED_FILE_SYSTEM.swigValue(),
            SyncError.UNSUPPORTED_FILE_SYSTEM,
        ),
        Arguments.of(Error.INVALID_REMOTE_TYPE.swigValue(), SyncError.INVALID_REMOTE_TYPE),
        Arguments.of(Error.INVALID_LOCAL_TYPE.swigValue(), SyncError.INVALID_LOCAL_TYPE),
        Arguments.of(Error.INITIAL_SCAN_FAILED.swigValue(), SyncError.INITIAL_SCAN_FAILED),
        Arguments.of(
            Error.LOCAL_PATH_TEMPORARY_UNAVAILABLE.swigValue(),
            SyncError.LOCAL_PATH_TEMPORARY_UNAVAILABLE,
        ),
        Arguments.of(
            Error.LOCAL_PATH_UNAVAILABLE.swigValue(),
            SyncError.LOCAL_PATH_UNAVAILABLE,
        ),
        Arguments.of(
            Error.REMOTE_NODE_NOT_FOUND.swigValue(),
            SyncError.REMOTE_NODE_NOT_FOUND,
        ),
        Arguments.of(Error.STORAGE_OVERQUOTA.swigValue(), SyncError.STORAGE_OVERQUOTA),
        Arguments.of(Error.ACCOUNT_EXPIRED.swigValue(), SyncError.ACCOUNT_EXPIRED),
        Arguments.of(
            Error.FOREIGN_TARGET_OVERSTORAGE.swigValue(),
            SyncError.FOREIGN_TARGET_OVERSTORAGE,
        ),
        Arguments.of(
            Error.REMOTE_PATH_HAS_CHANGED.swigValue(),
            SyncError.REMOTE_PATH_HAS_CHANGED,
        ),
        Arguments.of(
            Error.SHARE_NON_FULL_ACCESS.swigValue(),
            SyncError.SHARE_NON_FULL_ACCESS,
        ),
        Arguments.of(
            Error.LOCAL_FILESYSTEM_MISMATCH.swigValue(),
            SyncError.LOCAL_FILESYSTEM_MISMATCH,
        ),
        Arguments.of(Error.PUT_NODES_ERROR.swigValue(), SyncError.PUT_NODES_ERROR),
        Arguments.of(
            Error.ACTIVE_SYNC_BELOW_PATH.swigValue(),
            SyncError.ACTIVE_SYNC_BELOW_PATH,
        ),
        Arguments.of(
            Error.ACTIVE_SYNC_ABOVE_PATH.swigValue(),
            SyncError.ACTIVE_SYNC_ABOVE_PATH,
        ),
        Arguments.of(
            Error.REMOTE_NODE_MOVED_TO_RUBBISH.swigValue(),
            SyncError.REMOTE_NODE_MOVED_TO_RUBBISH,
        ),
        Arguments.of(
            Error.REMOTE_NODE_INSIDE_RUBBISH.swigValue(),
            SyncError.REMOTE_NODE_INSIDE_RUBBISH,
        ),
        Arguments.of(
            Error.VBOXSHAREDFOLDER_UNSUPPORTED.swigValue(),
            SyncError.VBOXSHAREDFOLDER_UNSUPPORTED,
        ),
        Arguments.of(
            Error.LOCAL_PATH_SYNC_COLLISION.swigValue(),
            SyncError.LOCAL_PATH_SYNC_COLLISION,
        ),
        Arguments.of(Error.ACCOUNT_BLOCKED.swigValue(), SyncError.ACCOUNT_BLOCKED),
        Arguments.of(
            Error.UNKNOWN_TEMPORARY_ERROR.swigValue(),
            SyncError.UNKNOWN_TEMPORARY_ERROR,
        ),
        Arguments.of(
            Error.TOO_MANY_ACTION_PACKETS.swigValue(),
            SyncError.TOO_MANY_ACTION_PACKETS,
        ),
        Arguments.of(Error.LOGGED_OUT.swigValue(), SyncError.LOGGED_OUT),
        Arguments.of(Error.BACKUP_MODIFIED.swigValue(), SyncError.BACKUP_MODIFIED),
        Arguments.of(
            Error.BACKUP_SOURCE_NOT_BELOW_DRIVE.swigValue(),
            SyncError.BACKUP_SOURCE_NOT_BELOW_DRIVE,
        ),
        Arguments.of(
            Error.SYNC_CONFIG_WRITE_FAILURE.swigValue(),
            SyncError.SYNC_CONFIG_WRITE_FAILURE,
        ),
        Arguments.of(
            Error.ACTIVE_SYNC_SAME_PATH.swigValue(),
            SyncError.ACTIVE_SYNC_SAME_PATH,
        ),
        Arguments.of(
            Error.COULD_NOT_MOVE_CLOUD_NODES.swigValue(),
            SyncError.COULD_NOT_MOVE_CLOUD_NODES,
        ),
        Arguments.of(
            Error.COULD_NOT_CREATE_IGNORE_FILE.swigValue(),
            SyncError.COULD_NOT_CREATE_IGNORE_FILE,
        ),
        Arguments.of(
            Error.SYNC_CONFIG_READ_FAILURE.swigValue(),
            SyncError.SYNC_CONFIG_READ_FAILURE,
        ),
        Arguments.of(Error.UNKNOWN_DRIVE_PATH.swigValue(), SyncError.UNKNOWN_DRIVE_PATH),
        Arguments.of(
            Error.INVALID_SCAN_INTERVAL.swigValue(),
            SyncError.INVALID_SCAN_INTERVAL,
        ),
        Arguments.of(
            Error.NOTIFICATION_SYSTEM_UNAVAILABLE.swigValue(),
            SyncError.NOTIFICATION_SYSTEM_UNAVAILABLE,
        ),
        Arguments.of(Error.UNABLE_TO_ADD_WATCH.swigValue(), SyncError.UNABLE_TO_ADD_WATCH),
        Arguments.of(
            Error.UNABLE_TO_RETRIEVE_ROOT_FSID.swigValue(),
            SyncError.UNABLE_TO_RETRIEVE_ROOT_FSID,
        ),
        Arguments.of(
            Error.UNABLE_TO_OPEN_DATABASE.swigValue(),
            SyncError.UNABLE_TO_OPEN_DATABASE,
        ),
        Arguments.of(
            Error.INSUFFICIENT_DISK_SPACE.swigValue(),
            SyncError.INSUFFICIENT_DISK_SPACE,
        ),
        Arguments.of(
            Error.FAILURE_ACCESSING_PERSISTENT_STORAGE.swigValue(),
            SyncError.FAILURE_ACCESSING_PERSISTENT_STORAGE,
        ),
        Arguments.of(
            Error.MISMATCH_OF_ROOT_FSID.swigValue(),
            SyncError.MISMATCH_OF_ROOT_FSID,
        ),
        Arguments.of(
            Error.FILESYSTEM_FILE_IDS_ARE_UNSTABLE.swigValue(),
            SyncError.FILESYSTEM_FILE_IDS_ARE_UNSTABLE,
        ),
        Arguments.of(
            Error.FILESYSTEM_ID_UNAVAILABLE.swigValue(),
            SyncError.FILESYSTEM_ID_UNAVAILABLE,
        ),
    )
}
package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth
import mega.privacy.android.feature.devicecenter.data.entity.SyncSubState
import nz.mega.sdk.MegaSync.Error
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [SyncSubStateMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncSubStateMapperTest {
    private lateinit var underTest: SyncSubStateMapper

    @BeforeAll
    fun setUp() {
        underTest = SyncSubStateMapper()
    }

    @ParameterizedTest(name = "when sdkSubState is {0}, then syncSubState is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(sdkSubState: Int, syncSubState: SyncSubState) {
        Truth.assertThat(underTest(sdkSubState)).isEqualTo(syncSubState)
    }

    @Test
    fun `test that an unknown value throws an exception`() {
        assertThrows<IllegalArgumentException> { underTest(123456) }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(Error.NO_SYNC_ERROR.swigValue(), SyncSubState.NO_SYNC_ERROR),
        Arguments.of(Error.UNKNOWN_ERROR.swigValue(), SyncSubState.UNKNOWN_ERROR),
        Arguments.of(
            Error.UNSUPPORTED_FILE_SYSTEM.swigValue(),
            SyncSubState.UNSUPPORTED_FILE_SYSTEM,
        ),
        Arguments.of(Error.INVALID_REMOTE_TYPE.swigValue(), SyncSubState.INVALID_REMOTE_TYPE),
        Arguments.of(Error.INVALID_LOCAL_TYPE.swigValue(), SyncSubState.INVALID_LOCAL_TYPE),
        Arguments.of(Error.INITIAL_SCAN_FAILED.swigValue(), SyncSubState.INITIAL_SCAN_FAILED),
        Arguments.of(
            Error.LOCAL_PATH_TEMPORARY_UNAVAILABLE.swigValue(),
            SyncSubState.LOCAL_PATH_TEMPORARY_UNAVAILABLE,
        ),
        Arguments.of(Error.LOCAL_PATH_UNAVAILABLE.swigValue(), SyncSubState.LOCAL_PATH_UNAVAILABLE),
        Arguments.of(Error.REMOTE_NODE_NOT_FOUND.swigValue(), SyncSubState.REMOTE_NODE_NOT_FOUND),
        Arguments.of(Error.STORAGE_OVERQUOTA.swigValue(), SyncSubState.STORAGE_OVERQUOTA),
        Arguments.of(Error.ACCOUNT_EXPIRED.swigValue(), SyncSubState.ACCOUNT_EXPIRED),
        Arguments.of(
            Error.FOREIGN_TARGET_OVERSTORAGE.swigValue(),
            SyncSubState.FOREIGN_TARGET_OVERSTORAGE,
        ),
        Arguments.of(
            Error.REMOTE_PATH_HAS_CHANGED.swigValue(),
            SyncSubState.REMOTE_PATH_HAS_CHANGED,
        ),
        Arguments.of(Error.SHARE_NON_FULL_ACCESS.swigValue(), SyncSubState.SHARE_NON_FULL_ACCESS),
        Arguments.of(
            Error.LOCAL_FILESYSTEM_MISMATCH.swigValue(),
            SyncSubState.LOCAL_FILESYSTEM_MISMATCH,
        ),
        Arguments.of(Error.PUT_NODES_ERROR.swigValue(), SyncSubState.PUT_NODES_ERROR),
        Arguments.of(Error.ACTIVE_SYNC_BELOW_PATH.swigValue(), SyncSubState.ACTIVE_SYNC_BELOW_PATH),
        Arguments.of(Error.ACTIVE_SYNC_ABOVE_PATH.swigValue(), SyncSubState.ACTIVE_SYNC_ABOVE_PATH),
        Arguments.of(
            Error.REMOTE_NODE_MOVED_TO_RUBBISH.swigValue(),
            SyncSubState.REMOTE_NODE_MOVED_TO_RUBBISH,
        ),
        Arguments.of(
            Error.REMOTE_NODE_INSIDE_RUBBISH.swigValue(),
            SyncSubState.REMOTE_NODE_INSIDE_RUBBISH,
        ),
        Arguments.of(
            Error.VBOXSHAREDFOLDER_UNSUPPORTED.swigValue(),
            SyncSubState.VBOXSHAREDFOLDER_UNSUPPORTED,
        ),
        Arguments.of(
            Error.LOCAL_PATH_SYNC_COLLISION.swigValue(),
            SyncSubState.LOCAL_PATH_SYNC_COLLISION,
        ),
        Arguments.of(Error.ACCOUNT_BLOCKED.swigValue(), SyncSubState.ACCOUNT_BLOCKED),
        Arguments.of(
            Error.UNKNOWN_TEMPORARY_ERROR.swigValue(),
            SyncSubState.UNKNOWN_TEMPORARY_ERROR,
        ),
        Arguments.of(
            Error.TOO_MANY_ACTION_PACKETS.swigValue(),
            SyncSubState.TOO_MANY_ACTION_PACKETS,
        ),
        Arguments.of(Error.LOGGED_OUT.swigValue(), SyncSubState.LOGGED_OUT),
        Arguments.of(
            Error.WHOLE_ACCOUNT_REFETCHED.swigValue(),
            SyncSubState.WHOLE_ACCOUNT_REFETCHED,
        ),
        Arguments.of(Error.MISSING_PARENT_NODE.swigValue(), SyncSubState.MISSING_PARENT_NODE),
        Arguments.of(Error.BACKUP_MODIFIED.swigValue(), SyncSubState.BACKUP_MODIFIED),
        Arguments.of(
            Error.BACKUP_SOURCE_NOT_BELOW_DRIVE.swigValue(),
            SyncSubState.BACKUP_SOURCE_NOT_BELOW_DRIVE,
        ),
        Arguments.of(
            Error.SYNC_CONFIG_WRITE_FAILURE.swigValue(),
            SyncSubState.SYNC_CONFIG_WRITE_FAILURE,
        ),
        Arguments.of(Error.ACTIVE_SYNC_SAME_PATH.swigValue(), SyncSubState.ACTIVE_SYNC_SAME_PATH),
        Arguments.of(
            Error.COULD_NOT_MOVE_CLOUD_NODES.swigValue(),
            SyncSubState.COULD_NOT_MOVE_CLOUD_NODES,
        ),
        Arguments.of(
            Error.COULD_NOT_CREATE_IGNORE_FILE.swigValue(),
            SyncSubState.COULD_NOT_CREATE_IGNORE_FILE,
        ),
        Arguments.of(
            Error.SYNC_CONFIG_READ_FAILURE.swigValue(),
            SyncSubState.SYNC_CONFIG_READ_FAILURE,
        ),
        Arguments.of(Error.UNKNOWN_DRIVE_PATH.swigValue(), SyncSubState.UNKNOWN_DRIVE_PATH),
        Arguments.of(Error.INVALID_SCAN_INTERVAL.swigValue(), SyncSubState.INVALID_SCAN_INTERVAL),
        Arguments.of(
            Error.NOTIFICATION_SYSTEM_UNAVAILABLE.swigValue(),
            SyncSubState.NOTIFICATION_SYSTEM_UNAVAILABLE,
        ),
        Arguments.of(Error.UNABLE_TO_ADD_WATCH.swigValue(), SyncSubState.UNABLE_TO_ADD_WATCH),
        Arguments.of(
            Error.UNABLE_TO_RETRIEVE_ROOT_FSID.swigValue(),
            SyncSubState.UNABLE_TO_RETRIEVE_ROOT_FSID,
        ),
        Arguments.of(
            Error.UNABLE_TO_OPEN_DATABASE.swigValue(),
            SyncSubState.UNABLE_TO_OPEN_DATABASE,
        ),
        Arguments.of(
            Error.INSUFFICIENT_DISK_SPACE.swigValue(),
            SyncSubState.INSUFFICIENT_DISK_SPACE,
        ),
        Arguments.of(
            Error.FAILURE_ACCESSING_PERSISTENT_STORAGE.swigValue(),
            SyncSubState.FAILURE_ACCESSING_PERSISTENT_STORAGE,
        ),
        Arguments.of(Error.MISMATCH_OF_ROOT_FSID.swigValue(), SyncSubState.MISMATCH_OF_ROOT_FSID),
        Arguments.of(
            Error.FILESYSTEM_FILE_IDS_ARE_UNSTABLE.swigValue(),
            SyncSubState.FILESYSTEM_FILE_IDS_ARE_UNSTABLE,
        ),
    )
}
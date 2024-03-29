package mega.privacy.android.shared.sync

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.shared.resources.R
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [DeviceFolderUINodeErrorMessageMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceFolderUINodeErrorMessageMapperTest {

    private lateinit var underTest: DeviceFolderUINodeErrorMessageMapper

    @BeforeAll
    fun setUp() {
        underTest = DeviceFolderUINodeErrorMessageMapper()
    }

    @ParameterizedTest(name = "when the error sub state is {0}, then the expected string res is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(
        errorSubState: SyncError,
        expectedStringRes: Int?,
    ) {
        assertThat(underTest(errorSubState)).isEqualTo(expectedStringRes)
    }

    @Test
    fun `test that a non matching sub state returns a default string res`() {
        assertThat(underTest(SyncError.UNKNOWN_BACKUP_INFO_SUB_STATE)).isEqualTo(R.string.general_sync_message_unknown_error)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            SyncError.NO_SYNC_ERROR,
            null,
        ),
        Arguments.of(
            SyncError.UNKNOWN_ERROR,
            R.string.general_sync_message_unknown_error,
        ),
        Arguments.of(
            SyncError.UNSUPPORTED_FILE_SYSTEM,
            R.string.general_sync_unsupported_file_system,
        ),
        Arguments.of(
            SyncError.INVALID_REMOTE_TYPE,
            R.string.general_sync_invalid_remote_type,
        ),
        Arguments.of(
            SyncError.INVALID_LOCAL_TYPE,
            R.string.general_sync_invalid_local_type,
        ),
        Arguments.of(
            SyncError.INITIAL_SCAN_FAILED,
            R.string.general_sync_initial_scan_failed,
        ),
        Arguments.of(
            SyncError.LOCAL_PATH_TEMPORARY_UNAVAILABLE,
            R.string.general_sync_message_cannot_locate_local_drive_now,
        ),
        Arguments.of(
            SyncError.LOCAL_PATH_UNAVAILABLE,
            R.string.general_sync_message_cannot_locate_local_drive,
        ),
        Arguments.of(
            SyncError.REMOTE_NODE_NOT_FOUND,
            R.string.general_sync_remote_node_not_found,
        ),
        Arguments.of(
            SyncError.STORAGE_OVERQUOTA,
            R.string.general_sync_storage_overquota,
        ),
        Arguments.of(
            SyncError.ACCOUNT_EXPIRED,
            R.string.general_sync_account_expired,
        ),
        Arguments.of(
            SyncError.FOREIGN_TARGET_OVERSTORAGE,
            R.string.general_sync_foreign_target_overshare,
        ),
        Arguments.of(
            SyncError.REMOTE_PATH_HAS_CHANGED,
            R.string.general_sync_remote_path_has_changed,
        ),
        Arguments.of(
            SyncError.SHARE_NON_FULL_ACCESS,
            R.string.general_sync_share_non_full_access,
        ),
        Arguments.of(
            SyncError.LOCAL_FILESYSTEM_MISMATCH,
            R.string.general_sync_local_filesystem_mismatch,
        ),
        Arguments.of(
            SyncError.PUT_NODES_ERROR,
            R.string.general_sync_put_nodes_error,
        ),
        Arguments.of(
            SyncError.ACTIVE_SYNC_BELOW_PATH,
            R.string.general_sync_active_sync_below_path,
        ),
        Arguments.of(
            SyncError.ACTIVE_SYNC_ABOVE_PATH,
            R.string.general_sync_message_folder_backup_issue_due_to_being_inside_another_backed_up_folder,
        ),
        Arguments.of(
            SyncError.REMOTE_NODE_MOVED_TO_RUBBISH,
            R.string.general_sync_message_node_in_rubbish_bin,
        ),
        Arguments.of(
            SyncError.REMOTE_NODE_INSIDE_RUBBISH,
            R.string.general_sync_message_node_in_rubbish_bin,
        ),
        Arguments.of(
            SyncError.VBOXSHAREDFOLDER_UNSUPPORTED,
            R.string.general_sync_vboxsharedfolder_unsupported,
        ),
        Arguments.of(
            SyncError.LOCAL_PATH_SYNC_COLLISION,
            R.string.general_sync_message_folder_backup_issue_due_to_being_inside_another_backed_up_folder,
        ),
        Arguments.of(
            SyncError.ACCOUNT_BLOCKED,
            R.string.general_sync_account_blocked,
        ),
        Arguments.of(
            SyncError.UNKNOWN_TEMPORARY_ERROR,
            R.string.general_sync_unknown_temporary_error,
        ),
        Arguments.of(
            SyncError.TOO_MANY_ACTION_PACKETS,
            R.string.general_sync_too_many_action_packets,
        ),
        Arguments.of(
            SyncError.LOGGED_OUT,
            R.string.general_sync_logged_out,
        ),
        Arguments.of(
            SyncError.BACKUP_MODIFIED,
            R.string.general_sync_message_folder_backup_issue_due_to_recent_changes,
        ),
        Arguments.of(
            SyncError.BACKUP_SOURCE_NOT_BELOW_DRIVE,
            R.string.general_sync_backup_source_not_below_drive,
        ),
        Arguments.of(
            SyncError.SYNC_CONFIG_WRITE_FAILURE,
            R.string.general_sync_message_folder_backup_issue,
        ),
        Arguments.of(
            SyncError.ACTIVE_SYNC_SAME_PATH,
            R.string.general_sync_active_sync_same_path,
        ),
        Arguments.of(
            SyncError.COULD_NOT_MOVE_CLOUD_NODES,
            R.string.general_sync_could_not_move_cloud_nodes,
        ),
        Arguments.of(
            SyncError.COULD_NOT_CREATE_IGNORE_FILE,
            R.string.general_sync_could_not_create_ignore_file,
        ),
        Arguments.of(
            SyncError.SYNC_CONFIG_READ_FAILURE,
            R.string.general_sync_config_read_failure,
        ),
        Arguments.of(
            SyncError.UNKNOWN_DRIVE_PATH,
            R.string.general_sync_unknown_drive_path,
        ),
        Arguments.of(
            SyncError.INVALID_SCAN_INTERVAL,
            R.string.general_sync_invalid_scan_interval,
        ),
        Arguments.of(
            SyncError.NOTIFICATION_SYSTEM_UNAVAILABLE,
            R.string.general_sync_notification_system_unavailable,
        ),
        Arguments.of(
            SyncError.UNABLE_TO_ADD_WATCH,
            R.string.general_sync_unable_to_add_watch,
        ),
        Arguments.of(
            SyncError.UNABLE_TO_RETRIEVE_ROOT_FSID,
            R.string.general_sync_unable_to_retrieve_root_fsid,
        ),
        Arguments.of(
            SyncError.UNABLE_TO_OPEN_DATABASE,
            R.string.general_sync_message_folder_backup_issue,
        ),
        Arguments.of(
            SyncError.INSUFFICIENT_DISK_SPACE,
            R.string.general_sync_insufficient_disk_space,
        ),
        Arguments.of(
            SyncError.FAILURE_ACCESSING_PERSISTENT_STORAGE,
            R.string.general_sync_unable_to_retrieve_root_fsid,
        ),
        Arguments.of(
            SyncError.MISMATCH_OF_ROOT_FSID,
            R.string.general_sync_message_folder_backup_issue,
        ),
        Arguments.of(
            SyncError.FILESYSTEM_FILE_IDS_ARE_UNSTABLE,
            R.string.general_sync_message_folder_backup_issue,
        ),
        Arguments.of(
            SyncError.FILESYSTEM_ID_UNAVAILABLE,
            R.string.general_sync_message_folder_backup_issue,
        ),
    )
}
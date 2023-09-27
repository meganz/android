package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfoSubState
import mega.privacy.android.feature.devicecenter.R
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
        errorSubState: BackupInfoSubState,
        expectedStringRes: Int,
    ) {
        assertThat(underTest(errorSubState)).isEqualTo(expectedStringRes)
    }

    @Test
    fun `test that a non matching sub state returns a default string res`() {
        assertThat(underTest(BackupInfoSubState.UNKNOWN_BACKUP_INFO_SUB_STATE)).isEqualTo(R.string.device_center_list_view_item_sub_state_message_unknown_error)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            BackupInfoSubState.NO_SYNC_ERROR,
            R.string.device_center_list_view_item_sub_state_no_sync_error,
        ),
        Arguments.of(
            BackupInfoSubState.UNKNOWN_ERROR,
            R.string.device_center_list_view_item_sub_state_message_unknown_error,
        ),
        Arguments.of(
            BackupInfoSubState.UNSUPPORTED_FILE_SYSTEM,
            R.string.device_center_list_view_item_sub_state_unsupported_file_system,
        ),
        Arguments.of(
            BackupInfoSubState.INVALID_REMOTE_TYPE,
            R.string.device_center_list_view_item_sub_state_invalid_remote_type,
        ),
        Arguments.of(
            BackupInfoSubState.INVALID_LOCAL_TYPE,
            R.string.device_center_list_view_item_sub_state_invalid_local_type,
        ),
        Arguments.of(
            BackupInfoSubState.INITIAL_SCAN_FAILED,
            R.string.device_center_list_view_item_sub_state_initial_scan_failed,
        ),
        Arguments.of(
            BackupInfoSubState.LOCAL_PATH_TEMPORARY_UNAVAILABLE,
            R.string.device_center_list_view_item_sub_state_message_cannot_locate_local_drive_now,
        ),
        Arguments.of(
            BackupInfoSubState.LOCAL_PATH_UNAVAILABLE,
            R.string.device_center_list_view_item_sub_state_message_cannot_locate_local_drive,
        ),
        Arguments.of(
            BackupInfoSubState.REMOTE_NODE_NOT_FOUND,
            R.string.device_center_list_view_item_sub_state_remote_node_not_found,
        ),
        Arguments.of(
            BackupInfoSubState.STORAGE_OVERQUOTA,
            R.string.device_center_list_view_item_sub_state_storage_overquota,
        ),
        Arguments.of(
            BackupInfoSubState.ACCOUNT_EXPIRED,
            R.string.device_center_list_view_item_sub_state_account_expired,
        ),
        Arguments.of(
            BackupInfoSubState.FOREIGN_TARGET_OVERSTORAGE,
            R.string.device_center_list_view_item_sub_state_foreign_target_overshare,
        ),
        Arguments.of(
            BackupInfoSubState.REMOTE_PATH_HAS_CHANGED,
            R.string.device_center_list_view_item_sub_state_remote_path_has_changed,
        ),
        Arguments.of(
            BackupInfoSubState.SHARE_NON_FULL_ACCESS,
            R.string.device_center_list_view_item_sub_state_share_non_full_access,
        ),
        Arguments.of(
            BackupInfoSubState.LOCAL_FILESYSTEM_MISMATCH,
            R.string.device_center_list_view_item_sub_state_message_something_went_wrong,
        ),
        Arguments.of(
            BackupInfoSubState.PUT_NODES_ERROR,
            R.string.device_center_list_view_item_sub_state_put_nodes_error,
        ),
        Arguments.of(
            BackupInfoSubState.ACTIVE_SYNC_BELOW_PATH,
            R.string.device_center_list_view_item_sub_state_active_sync_below_path,
        ),
        Arguments.of(
            BackupInfoSubState.ACTIVE_SYNC_ABOVE_PATH,
            R.string.device_center_list_view_item_sub_state_message_folder_backup_issue_due_to_being_inside_another_backed_up_folder,
        ),
        Arguments.of(
            BackupInfoSubState.REMOTE_NODE_MOVED_TO_RUBBISH,
            R.string.device_center_list_view_item_sub_state_message_node_in_rubbish_bin,
        ),
        Arguments.of(
            BackupInfoSubState.REMOTE_NODE_INSIDE_RUBBISH,
            R.string.device_center_list_view_item_sub_state_message_node_in_rubbish_bin,
        ),
        Arguments.of(
            BackupInfoSubState.VBOXSHAREDFOLDER_UNSUPPORTED,
            R.string.device_center_list_view_item_sub_state_vboxsharedfolder_unsupported,
        ),
        Arguments.of(
            BackupInfoSubState.LOCAL_PATH_SYNC_COLLISION,
            R.string.device_center_list_view_item_sub_state_message_folder_backup_issue_due_to_being_inside_another_backed_up_folder,
        ),
        Arguments.of(
            BackupInfoSubState.ACCOUNT_BLOCKED,
            R.string.device_center_list_view_item_sub_state_account_blocked,
        ),
        Arguments.of(
            BackupInfoSubState.UNKNOWN_TEMPORARY_ERROR,
            R.string.device_center_list_view_item_sub_state_unknown_temporary_error,
        ),
        Arguments.of(
            BackupInfoSubState.TOO_MANY_ACTION_PACKETS,
            R.string.device_center_list_view_item_sub_state_too_many_action_packets,
        ),
        Arguments.of(
            BackupInfoSubState.LOGGED_OUT,
            R.string.device_center_list_view_item_sub_state_logged_out,
        ),
        Arguments.of(
            BackupInfoSubState.MISSING_PARENT_NODE,
            R.string.device_center_list_view_item_sub_state_missing_parent_node,
        ),
        Arguments.of(
            BackupInfoSubState.BACKUP_MODIFIED,
            R.string.device_center_list_view_item_sub_state_message_folder_backup_issue_due_to_recent_changes,
        ),
        Arguments.of(
            BackupInfoSubState.BACKUP_SOURCE_NOT_BELOW_DRIVE,
            R.string.device_center_list_view_item_sub_state_backup_source_not_below_drive,
        ),
        Arguments.of(
            BackupInfoSubState.SYNC_CONFIG_WRITE_FAILURE,
            R.string.device_center_list_view_item_sub_state_message_folder_backup_issue,
        ),
        Arguments.of(
            BackupInfoSubState.ACTIVE_SYNC_SAME_PATH,
            R.string.device_center_list_view_item_sub_state_active_sync_same_path,
        ),
        Arguments.of(
            BackupInfoSubState.COULD_NOT_MOVE_CLOUD_NODES,
            R.string.device_center_list_view_item_sub_state_could_not_move_cloud_nodes,
        ),
        Arguments.of(
            BackupInfoSubState.COULD_NOT_CREATE_IGNORE_FILE,
            R.string.device_center_list_view_item_sub_state_could_not_create_ignore_file,
        ),
        Arguments.of(
            BackupInfoSubState.SYNC_CONFIG_READ_FAILURE,
            R.string.device_center_list_view_item_sub_state_sync_config_read_failure,
        ),
        Arguments.of(
            BackupInfoSubState.UNKNOWN_DRIVE_PATH,
            R.string.device_center_list_view_item_sub_state_unknown_drive_path,
        ),
        Arguments.of(
            BackupInfoSubState.INVALID_SCAN_INTERVAL,
            R.string.device_center_list_view_item_sub_state_invalid_scan_interval,
        ),
        Arguments.of(
            BackupInfoSubState.NOTIFICATION_SYSTEM_UNAVAILABLE,
            R.string.device_center_list_view_item_sub_state_notification_system_unavailable,
        ),
        Arguments.of(
            BackupInfoSubState.UNABLE_TO_ADD_WATCH,
            R.string.device_center_list_view_item_sub_state_unable_to_add_watch,
        ),
        Arguments.of(
            BackupInfoSubState.UNABLE_TO_RETRIEVE_ROOT_FSID,
            R.string.device_center_list_view_item_sub_state_unable_to_retrieve_root_fsid,
        ),
        Arguments.of(
            BackupInfoSubState.UNABLE_TO_OPEN_DATABASE,
            R.string.device_center_list_view_item_sub_state_unable_to_open_database,
        ),
        Arguments.of(
            BackupInfoSubState.INSUFFICIENT_DISK_SPACE,
            R.string.device_center_list_view_item_sub_state_insufficient_disk_space,
        ),
        Arguments.of(
            BackupInfoSubState.FAILURE_ACCESSING_PERSISTENT_STORAGE,
            R.string.device_center_list_view_item_sub_state_failure_accessing_persistent_storage,
        ),
        Arguments.of(
            BackupInfoSubState.MISMATCH_OF_ROOT_FSID,
            R.string.device_center_list_view_item_sub_state_message_something_went_wrong,
        ),
        Arguments.of(
            BackupInfoSubState.FILESYSTEM_FILE_IDS_ARE_UNSTABLE,
            R.string.device_center_list_view_item_sub_state_message_something_went_wrong,
        ),
        Arguments.of(
            BackupInfoSubState.FILESYSTEM_ID_UNAVAILABLE,
            R.string.device_center_list_view_item_sub_state_message_something_went_wrong,
        ),
    )
}
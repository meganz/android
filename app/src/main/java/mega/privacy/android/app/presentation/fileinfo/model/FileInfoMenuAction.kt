package mega.privacy.android.app.presentation.fileinfo.model

import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionPlural
import mega.android.core.ui.model.menu.MenuActionString
import mega.android.core.ui.model.menu.MenuActionWithoutIcon
import mega.privacy.android.shared.resources.R as sharedR

internal sealed interface FileInfoMenuAction : MenuAction {

    object Download : MenuActionString(
        IconPack.Medium.Thin.Outline.Download,
        R.string.general_save_to_device,
        TEST_TAG_DOWNLOAD_ACTION
    ), FileInfoMenuAction {
        override val orderInCategory = 95
    }

    object ShareFolder : MenuActionString(
        IconPack.Medium.Thin.Outline.FolderUsers,
        R.string.context_share_folder,
        TEST_TAG_SHARE_FOLDER_ACTION,
    ), FileInfoMenuAction

    object GetLink : MenuActionPlural(
        IconPack.Medium.Thin.Solid.Link01,
        sharedR.plurals.label_share_links, 1,
        TEST_TAG_GET_LINK_ACTION,
    ), FileInfoMenuAction

    object SendToChat : MenuActionString(
        IconPack.Medium.Thin.Outline.MessageArrowUp,
        R.string.context_send_file_to_chat,
        TEST_TAG_SEND_CHAT_ACTION,
    ), FileInfoMenuAction {
        override val orderInCategory = 115
    }

    object ManageLink : MenuActionWithoutIcon(
        R.string.edit_link_option,
        TEST_TAG_MANAGE_LINK_ACTION,
    ), FileInfoMenuAction {
        override val orderInCategory = 95
    }

    object RemoveLink : MenuActionString(
        IconPack.Medium.Thin.Outline.LinkOff01,
        R.string.context_remove_link_menu,
        TEST_TAG_REMOVE_LINK_ACTION
    ), FileInfoMenuAction

    object DisputeTakedown : MenuActionString(
        IconPack.Medium.Thin.Outline.AlertTriangle,
        R.string.dispute_takendown_file,
        TEST_TAG_DISPUTE_TAKE_DOWN_ACTION
    ), FileInfoMenuAction {
        override val orderInCategory = 105
    }

    object Rename : MenuActionString(
        IconPack.Medium.Thin.Outline.Pen2,
        R.string.context_rename,
        TEST_TAG_RENAME_ACTION
    ), FileInfoMenuAction {
        override val orderInCategory = 110
    }

    object Move : MenuActionString(
        IconPack.Medium.Thin.Outline.Move,
        R.string.general_move,
        TEST_TAG_MOVE_ACTION
    ), FileInfoMenuAction {
        override val orderInCategory = 120
    }

    object Copy : MenuActionString(
        IconPack.Medium.Thin.Outline.Copy01,
        R.string.context_copy,
        TEST_TAG_COPY_ACTION
    ), FileInfoMenuAction {
        override val orderInCategory = 120
    }

    object MoveToRubbishBin : MenuActionString(
        IconPack.Medium.Thin.Outline.Trash,
        R.string.context_move_to_trash,
        TEST_TAG_RUBBISH_ACTION
    ), FileInfoMenuAction {
        override val orderInCategory = 130
    }

    object Leave : MenuActionString(
        IconPack.Medium.Thin.Outline.LogOut02,
        R.string.general_leave,
        TEST_TAG_LEAVE_ACTION
    ), FileInfoMenuAction {
        override val orderInCategory = 130
    }

    object Delete : MenuActionWithoutIcon(
        R.string.general_remove,
        TEST_TAG_REMOVE_ACTION
    ), FileInfoMenuAction {
        override val orderInCategory = 130
    }

    sealed interface SelectionModeAction : FileInfoMenuAction {
        object SelectAll : MenuActionWithoutIcon(
            R.string.action_select_all,
            TEST_TAG_SELECT_ALL_SELECT_MODE_ACTION
        ), SelectionModeAction

        object ClearSelection : MenuActionWithoutIcon(
            R.string.action_unselect_all,
            TEST_TAG_CLEAR_SELECTION_SELECT_MODE_ACTION
        ), SelectionModeAction

        object ChangePermission : MenuActionString(
            IconPack.Medium.Thin.Outline.Key02,
            R.string.file_properties_shared_folder_change_permissions,
            TEST_TAG_CHANGE_PERMISSION_SELECT_MODE_ACTION
        ), SelectionModeAction

        object Remove : MenuActionString(
            IconPack.Medium.Thin.Outline.X,
            R.string.context_remove,
            TEST_TAG_REMOVE_SELECT_MODE_ACTION
        ), SelectionModeAction

        companion object {
            fun all() = listOf(SelectAll, ClearSelection, ChangePermission, Remove)
            const val TEST_TAG_SELECT_ALL_SELECT_MODE_ACTION =
                "file_info_view:select_mode_action_select_all"
            const val TEST_TAG_CLEAR_SELECTION_SELECT_MODE_ACTION =
                "file_info_view:select_mode_action_clear_selection"
            const val TEST_TAG_CHANGE_PERMISSION_SELECT_MODE_ACTION =
                "file_info_view:select_mode_action_change_permission"
            const val TEST_TAG_REMOVE_SELECT_MODE_ACTION =
                "file_info_view:select_mode_action_remove"
        }
    }

    companion object {
        const val TEST_TAG_DOWNLOAD_ACTION = "file_info_view:action_download"
        const val TEST_TAG_SHARE_FOLDER_ACTION = "file_info_view:action_share_folder"
        const val TEST_TAG_GET_LINK_ACTION = "file_info_view:action_get_link"
        const val TEST_TAG_SEND_CHAT_ACTION = "file_info_view:action_send_chat"
        const val TEST_TAG_MANAGE_LINK_ACTION = "file_info_view:action_manage_link"
        const val TEST_TAG_REMOVE_LINK_ACTION = "file_info_view:action_remove_link"
        const val TEST_TAG_DISPUTE_TAKE_DOWN_ACTION = "file_info_view:action_dispute_take_down"
        const val TEST_TAG_RENAME_ACTION = "file_info_view:action_rename"
        const val TEST_TAG_MOVE_ACTION = "file_info_view:action_move"
        const val TEST_TAG_COPY_ACTION = "file_info_view:action_copy"
        const val TEST_TAG_RUBBISH_ACTION = "file_info_view:action_rubbish_bin"
        const val TEST_TAG_LEAVE_ACTION = "file_info_view:action_leave"
        const val TEST_TAG_REMOVE_ACTION = "file_info_view:action_remove"
    }
}
package mega.privacy.android.app.presentation.fileinfo.model

import mega.privacy.android.app.R
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionPlural
import mega.privacy.android.core.ui.model.MenuActionString
import mega.privacy.android.core.ui.model.MenuActionWithoutIcon

internal sealed interface FileInfoMenuAction : MenuAction {
    object Download : MenuActionString(
        R.drawable.ic_download_white,
        R.string.general_save_to_device,
    ), FileInfoMenuAction

    object ShareFolder : MenuActionString(
        R.drawable.ic_share,
        R.string.context_share_folder,
    ), FileInfoMenuAction

    object GetLink : MenuActionPlural(
        R.drawable.link_ic_white,
        R.plurals.get_links, 1
    ), FileInfoMenuAction

    object SendToChat : MenuActionString(
        R.drawable.ic_send_to_contact,
        R.string.context_send_file_to_chat,
    ), FileInfoMenuAction

    object ManageLink : MenuActionWithoutIcon(
        R.string.edit_link_option,
    ), FileInfoMenuAction

    object RemoveLink : MenuActionString(
        R.drawable.ic_remove_link,
        R.string.context_remove_link_menu,
    ), FileInfoMenuAction

    object DisputeTakedown : MenuActionString(
        R.drawable.ic_taken_down_file_info,
        R.string.dispute_takendown_file,
    ), FileInfoMenuAction

    object Rename : MenuActionString(
        R.drawable.ic_rename,
        R.string.context_rename,
    ), FileInfoMenuAction

    object Move : MenuActionString(
        R.drawable.ic_move_white,
        R.string.general_move,
    ), FileInfoMenuAction

    object Copy : MenuActionString(
        R.drawable.ic_copy_white,
        R.string.context_copy,
    ), FileInfoMenuAction

    object MoveToRubbishBin : MenuActionString(
        R.drawable.ic_move_to_rubbish_bin,
        R.string.context_move_to_trash,
    ), FileInfoMenuAction

    object Leave : MenuActionString(
        R.drawable.ic_leave_share_w,
        R.string.general_leave,
    ), FileInfoMenuAction

    object Delete : MenuActionWithoutIcon(
        R.string.general_remove,
    ), FileInfoMenuAction
}
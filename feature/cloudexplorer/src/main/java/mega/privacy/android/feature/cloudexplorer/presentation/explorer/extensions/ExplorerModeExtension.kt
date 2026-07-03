package mega.privacy.android.feature.cloudexplorer.presentation.explorer.extensions

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Presentation-layer extension properties for [ExplorerMode].
 *
 * These extensions map each explorer mode to the appropriate UI string resources
 * used in the explorer screen's top bar title and primary action button.
 */

/**
 * The string resource ID for the top app bar title.
 *
 * File-picker modes (e.g., [ExplorerMode.ShareFilesToChat]) show a "selected" title,
 * while folder-picker modes show a "select destination" title.
 */
@get:StringRes
val ExplorerMode.titleStringId: Int
    get() = when (this) {
        ExplorerMode.ShareFilesToChat,
        ExplorerMode.AddVideosToPlaylist,
            -> sharedR.string.video_section_video_selected_top_bar_title

        ExplorerMode.PinToHome -> sharedR.string.home_pinned_choose_files_and_folders

        else -> sharedR.string.cloud_explorer_select_destination_title
    }

/**
 * The string resource ID for the primary action button label.
 *
 * Each mode maps to the verb that best describes its action:
 * upload, send, move, copy, save, or add.
 */
@get:StringRes
val ExplorerMode.actionStringId: Int
    get() = when (this) {
        ExplorerMode.ShareFilesToMega,
        ExplorerMode.ShareTextToMega,
        ExplorerMode.ShareURLToMega,
        ExplorerMode.SaveScannedDocument,
            -> sharedR.string.general_upload_label

        ExplorerMode.ShareFilesToChat -> sharedR.string.context_send
        ExplorerMode.Move -> sharedR.string.general_move
        ExplorerMode.Copy -> sharedR.string.general_copy
        ExplorerMode.SelectCUFolder, ExplorerMode.SelectSyncFolder, ExplorerMode.SelectStopBackupDestination -> sharedR.string.cloud_explorer_use_this_folder_button

        ExplorerMode.Import,
        ExplorerMode.AlbumImport,
            -> sharedR.string.general_action_save

        ExplorerMode.AddVideosToPlaylist -> sharedR.string.video_to_playlist_add_button
        ExplorerMode.PinToHome -> sharedR.string.general_dialog_choose_button
    }

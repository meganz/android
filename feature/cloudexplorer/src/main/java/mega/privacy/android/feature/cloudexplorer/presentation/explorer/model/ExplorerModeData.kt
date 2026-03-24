package mega.privacy.android.feature.cloudexplorer.presentation.explorer.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.shared.resources.R as sharedR

sealed class ExplorerModeData(
    val isFolderPicker: Boolean,
    @StringRes val titleStringId: Int,
    @StringRes val actionStringId: Int,
    val isIncomingAvailable: Boolean,
    val isChatAvailable: Boolean,
) {

    sealed class Upload : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_upload_label,
        //Update with new string "Select destination"
        titleStringId = sharedR.string.general_select_folder,
        isIncomingAvailable = true,
        isChatAvailable = true,
    )

    data object ShareFilesToMega : Upload()

    data object ShareTextToMega : Upload()

    data object ShareURLToMega : Upload()

    data object SaveScannedDocument : Upload()

    data object ShareFilesToChat : ExplorerModeData(
        isFolderPicker = false,
        actionStringId = sharedR.string.context_send,
        titleStringId = sharedR.string.video_section_video_selected_top_bar_title,
        isIncomingAvailable = true,
        isChatAvailable = false,
    )

    data object Move : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_move,
        //Update with new string "Select destination"
        titleStringId = sharedR.string.general_select_folder,
        isIncomingAvailable = true,
        isChatAvailable = false,
    )

    data object Copy : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_copy,
        //Update with new string "Select destination"
        titleStringId = sharedR.string.general_select_folder,
        isIncomingAvailable = true,
        isChatAvailable = false,
    )

    data object SelectCUFolder : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_select,
        //Update with new string "Choose folder"
        titleStringId = sharedR.string.general_select_folder,
        isIncomingAvailable = true,
        isChatAvailable = false,
    )

    data object Import : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_action_save,
        //Update with new string "Select destination"
        titleStringId = sharedR.string.general_select_folder,
        isIncomingAvailable = true,
        isChatAvailable = false,
    )

    data object AlbumImport : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_action_save,
        //Update with new string "Select destination"
        titleStringId = sharedR.string.general_select_folder,
        isIncomingAvailable = false,
        isChatAvailable = false,
    )

    data object AddVideosToPlaylist : ExplorerModeData(
        isFolderPicker = false,
        actionStringId = sharedR.string.video_to_playlist_add_button,
        titleStringId = sharedR.string.video_section_video_selected_top_bar_title,
        isIncomingAvailable = false,
        isChatAvailable = false,
    )
}

fun ExplorerMode.toData() = when (this) {
    ExplorerMode.ShareFilesToMega -> ExplorerModeData.ShareFilesToMega
    ExplorerMode.ShareTextToMega -> ExplorerModeData.ShareTextToMega
    ExplorerMode.ShareURLToMega -> ExplorerModeData.ShareURLToMega
    ExplorerMode.SaveScannedDocument -> ExplorerModeData.SaveScannedDocument
    ExplorerMode.ShareFilesToChat -> ExplorerModeData.ShareFilesToChat
    ExplorerMode.Move -> ExplorerModeData.Move
    ExplorerMode.Copy -> ExplorerModeData.Copy
    ExplorerMode.SelectCUFolder -> ExplorerModeData.SelectCUFolder
    ExplorerMode.Import -> ExplorerModeData.Import
    ExplorerMode.AlbumImport -> ExplorerModeData.AlbumImport
    ExplorerMode.AddVideosToPlaylist -> ExplorerModeData.AddVideosToPlaylist
}

fun ExplorerModeData.toMode() = when (this) {
    ExplorerModeData.ShareFilesToMega -> ExplorerMode.ShareFilesToMega
    ExplorerModeData.ShareTextToMega -> ExplorerMode.ShareTextToMega
    ExplorerModeData.ShareURLToMega -> ExplorerMode.ShareURLToMega
    ExplorerModeData.SaveScannedDocument -> ExplorerMode.SaveScannedDocument
    ExplorerModeData.ShareFilesToChat -> ExplorerMode.ShareFilesToChat
    ExplorerModeData.Move -> ExplorerMode.Move
    ExplorerModeData.Copy -> ExplorerMode.Copy
    ExplorerModeData.SelectCUFolder -> ExplorerMode.SelectCUFolder
    ExplorerModeData.Import -> ExplorerMode.Import
    ExplorerModeData.AlbumImport -> ExplorerMode.AlbumImport
    ExplorerModeData.AddVideosToPlaylist -> ExplorerMode.AddVideosToPlaylist
}
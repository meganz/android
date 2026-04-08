package mega.privacy.android.feature.cloudexplorer.presentation.explorer.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.shared.resources.R as sharedR

sealed class ExplorerModeData(
    val isFolderPicker: Boolean,
    @StringRes val titleStringId: Int,
    @StringRes val actionStringId: Int,
    val isIncomingAvailable: Boolean,
    val isChatAvailable: Boolean,
    open val startNavKey: ExplorerNavKey,
) {

    sealed class Upload(override val startNavKey: ExplorerNavKey) : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_upload_label,
        titleStringId = sharedR.string.cloud_explorer_select_destination_title,
        isIncomingAvailable = true,
        isChatAvailable = true,
        startNavKey = startNavKey,
    )

    data class ShareFilesToMega(
        override val startNavKey: ExplorerNavKey,
        val shareUris: List<UriPath>,
    ) : Upload(startNavKey)

    data class ShareTextToMega(override val startNavKey: ExplorerNavKey) : Upload(startNavKey)

    data class ShareURLToMega(override val startNavKey: ExplorerNavKey) : Upload(startNavKey)

    data class SaveScannedDocument(override val startNavKey: ExplorerNavKey) : Upload(startNavKey)

    data class ShareFilesToChat(override val startNavKey: ExplorerNavKey) : ExplorerModeData(
        isFolderPicker = false,
        actionStringId = sharedR.string.context_send,
        titleStringId = sharedR.string.video_section_video_selected_top_bar_title,
        isIncomingAvailable = true,
        isChatAvailable = false,
        startNavKey = startNavKey,
    )

    data class Move(override val startNavKey: ExplorerNavKey) : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_move,
        titleStringId = sharedR.string.cloud_explorer_select_destination_title,
        isIncomingAvailable = true,
        isChatAvailable = false,
        startNavKey = startNavKey,
    )

    data class Copy(override val startNavKey: ExplorerNavKey) : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_copy,
        titleStringId = sharedR.string.cloud_explorer_select_destination_title,
        isIncomingAvailable = true,
        isChatAvailable = false,
        startNavKey = startNavKey,
    )

    data class SelectCUFolder(override val startNavKey: ExplorerNavKey) : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.cloud_explorer_use_this_folder_button,
        titleStringId = sharedR.string.cloud_explorer_select_destination_title,
        isIncomingAvailable = true,
        isChatAvailable = false,
        startNavKey = startNavKey,
    )

    data class Import(override val startNavKey: ExplorerNavKey) : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_action_save,
        titleStringId = sharedR.string.cloud_explorer_select_destination_title,
        isIncomingAvailable = true,
        isChatAvailable = false,
        startNavKey = startNavKey,
    )

    data class AlbumImport(override val startNavKey: ExplorerNavKey) : ExplorerModeData(
        isFolderPicker = true,
        actionStringId = sharedR.string.general_action_save,
        titleStringId = sharedR.string.cloud_explorer_select_destination_title,
        isIncomingAvailable = false,
        isChatAvailable = false,
        startNavKey = startNavKey,
    )

    data class AddVideosToPlaylist(override val startNavKey: ExplorerNavKey) : ExplorerModeData(
        isFolderPicker = false,
        actionStringId = sharedR.string.video_to_playlist_add_button,
        titleStringId = sharedR.string.video_section_video_selected_top_bar_title,
        isIncomingAvailable = false,
        isChatAvailable = false,
        startNavKey = startNavKey,
    )
}

fun ExplorerMode.toData(explorerKey: NodesExplorerNavKey) = when (this) {
    ExplorerMode.ShareFilesToMega -> ExplorerModeData.ShareFilesToMega(
        startNavKey = explorerKey.startNavKey,
        shareUris = explorerKey.shareUris ?: emptyList()
    )

    ExplorerMode.ShareTextToMega -> ExplorerModeData.ShareTextToMega(explorerKey.startNavKey)
    ExplorerMode.ShareURLToMega -> ExplorerModeData.ShareURLToMega(explorerKey.startNavKey)
    ExplorerMode.SaveScannedDocument -> ExplorerModeData.SaveScannedDocument(explorerKey.startNavKey)
    ExplorerMode.ShareFilesToChat -> ExplorerModeData.ShareFilesToChat(explorerKey.startNavKey)
    ExplorerMode.Move -> ExplorerModeData.Move(explorerKey.startNavKey)
    ExplorerMode.Copy -> ExplorerModeData.Copy(explorerKey.startNavKey)
    ExplorerMode.SelectCUFolder -> ExplorerModeData.SelectCUFolder(explorerKey.startNavKey)
    ExplorerMode.Import -> ExplorerModeData.Import(explorerKey.startNavKey)
    ExplorerMode.AlbumImport -> ExplorerModeData.AlbumImport(explorerKey.startNavKey)
    ExplorerMode.AddVideosToPlaylist -> ExplorerModeData.AddVideosToPlaylist(explorerKey.startNavKey)
}

fun ExplorerModeData.toMode() = when (this) {
    is ExplorerModeData.ShareFilesToMega -> ExplorerMode.ShareFilesToMega
    is ExplorerModeData.ShareTextToMega -> ExplorerMode.ShareTextToMega
    is ExplorerModeData.ShareURLToMega -> ExplorerMode.ShareURLToMega
    is ExplorerModeData.SaveScannedDocument -> ExplorerMode.SaveScannedDocument
    is ExplorerModeData.ShareFilesToChat -> ExplorerMode.ShareFilesToChat
    is ExplorerModeData.Move -> ExplorerMode.Move
    is ExplorerModeData.Copy -> ExplorerMode.Copy
    is ExplorerModeData.SelectCUFolder -> ExplorerMode.SelectCUFolder
    is ExplorerModeData.Import -> ExplorerMode.Import
    is ExplorerModeData.AlbumImport -> ExplorerMode.AlbumImport
    is ExplorerModeData.AddVideosToPlaylist -> ExplorerMode.AddVideosToPlaylist
}
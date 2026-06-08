package mega.privacy.android.domain.entity.cloudexplorer

/**
 * Represents the different modes in which the cloud explorer can operate.
 *
 * Each mode defines the behavior and available navigation tabs of the explorer screen
 * based on the user's intent (e.g., uploading files, moving nodes, selecting a folder).
 *
 * @property isFolderPicker Whether the explorer acts as a folder picker (selecting a destination folder)
 *   rather than a file picker (selecting individual files).
 * @property isIncomingAvailable Whether the Incoming Shares tab is available in this mode.
 * @property isChatAvailable Whether the Chat tab is available in this mode.
 * @property isVideoPicker Whether only video files can be selected. Non-video files are shown disabled.
 */
enum class ExplorerMode(
    val isFolderPicker: Boolean,
    val isIncomingAvailable: Boolean,
    val isChatAvailable: Boolean,
    val isVideoPicker: Boolean,
) {
    /** Upload files shared from an external app to a MEGA folder. */
    ShareFilesToMega(
        isFolderPicker = true,
        isIncomingAvailable = true,
        isChatAvailable = true,
        isVideoPicker = false,
    ),

    /** Upload text shared from an external app to a MEGA folder. */
    ShareTextToMega(
        isFolderPicker = true,
        isIncomingAvailable = true,
        isChatAvailable = true,
        isVideoPicker = false,
    ),

    /** Upload a URL shared from an external app to a MEGA folder. */
    ShareURLToMega(
        isFolderPicker = true,
        isIncomingAvailable = true,
        isChatAvailable = true,
        isVideoPicker = false,
    ),

    /** Save a scanned document to a MEGA folder. */
    SaveScannedDocument(
        isFolderPicker = true,
        isIncomingAvailable = true,
        isChatAvailable = true,
        isVideoPicker = false,
    ),

    /** Select files to send to a chat conversation. */
    ShareFilesToChat(
        isFolderPicker = false,
        isIncomingAvailable = true,
        isChatAvailable = false,
        isVideoPicker = false,
    ),

    /** Select a destination folder to move nodes into. */
    Move(
        isFolderPicker = true,
        isIncomingAvailable = true,
        isChatAvailable = false,
        isVideoPicker = false,
    ),

    /** Select a destination folder to copy nodes into. */
    Copy(
        isFolderPicker = true,
        isIncomingAvailable = true,
        isChatAvailable = false,
        isVideoPicker = false,
    ),

    /** Select a folder for Camera Uploads. */
    SelectCUFolder(
        isFolderPicker = true,
        isIncomingAvailable = true,
        isChatAvailable = false,
        isVideoPicker = false,
    ),

    /** Select a destination folder to import nodes from a public link. */
    Import(
        isFolderPicker = true,
        isIncomingAvailable = true,
        isChatAvailable = false,
        isVideoPicker = false,
    ),

    /** Select a destination folder to import an album. */
    AlbumImport(
        isFolderPicker = true,
        isIncomingAvailable = false,
        isChatAvailable = false,
        isVideoPicker = false,
    ),

    /** Select videos to add to a playlist. */
    AddVideosToPlaylist(
        isFolderPicker = false,
        isIncomingAvailable = false,
        isChatAvailable = false,
        isVideoPicker = true,
    ),
}
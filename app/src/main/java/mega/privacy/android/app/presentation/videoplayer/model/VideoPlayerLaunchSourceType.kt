package mega.privacy.android.app.presentation.videoplayer.model

/**
 * Where the video player was launched from.
 *
 * The player is launched with a legacy adapter-type [Int]. Those integers come from two separate
 * namespaces — `app.utils.Constants` and `shared.nodes.model.NodeSourceTypeInt` — which assign
 * different meanings to the same value (2035 is `VERSIONS_ADAPTER` in one and
 * `VIDEO_PLAYLISTS_ADAPTER` in the other, 2036 is `FROM_IMAGE_VIEWER` against
 * `VIDEO_RECENTLY_WATCHED_ADAPTER`). Matching on the integer is therefore only correct as long as
 * every caller happens to mean the same namespace, which nothing enforces.
 */
enum class VideoPlayerLaunchSourceType {
    /** Files saved for offline. */
    Offline,

    /** A video inside a zip archive. */
    Zip,

    /** Cloud drive. */
    FileBrowser,

    /** Rubbish bin. */
    RubbishBin,

    /** Backups. */
    Backups,

    /** Shared links. */
    Links,

    /** Incoming shares. */
    IncomingShares,

    /** Outgoing shares. */
    OutgoingShares,

    /** Favourites. */
    Favourites,

    /** A contact's shared files. */
    ContactFile,

    /** A public folder link. */
    FolderLink,

    /** A public file link. */
    FileLink,

    /** A chat attachment. */
    Chat,

    /** The image viewer. */
    ImageViewer,

    /** Media discovery. */
    MediaDiscovery,

    /** Album sharing. */
    AlbumSharing,

    /** The version history of a node. */
    Versions,

    /** Recents. */
    Recents,

    /** A single bucket within recents. */
    RecentsBucket,

    /** Search results. */
    Search,

    /** The video section. */
    VideoBrowse,

    /**
     * An adapter type this player does not recognise. Such launches are still allowed through and
     * fall back to the general-source path, matching the previous behaviour — only the explicit
     * `INVALID_VALUE` sentinel rejects a launch.
     */
    Unknown,
}

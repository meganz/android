package mega.privacy.android.app.presentation.videoplayer.model

import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.FROM_MEDIA_DISCOVERY
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_BUCKET_ADAPTER
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSourceType
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.BACKUPS_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FAVOURITES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FILE_BROWSER_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.FILE_LINK_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.INCOMING_SHARES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.LINKS_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER

/**
 * Maps a legacy adapter-type [Int] to a [VideoPlayerLaunchSourceType].
 *
 * The integers arrive from two namespaces that overlap numerically: `app.utils.Constants` and
 * `shared.nodes.model.NodeSourceTypeInt`. Where they collide, this mapping takes the
 * `Constants` meaning, which is the one the video player reads:
 * - 2035 is read as `VERSIONS_ADAPTER`, not `VIDEO_PLAYLISTS_ADAPTER`
 * - 2036 is read as `FROM_IMAGE_VIEWER`, not `VIDEO_RECENTLY_WATCHED_ADAPTER`
 *
 * The `INVALID_VALUE` sentinel becomes [VideoPlayerLaunchSourceType.Invalid], which is the only
 * value that rejects a launch. Anything else unrecognised becomes
 * [VideoPlayerLaunchSourceType.Unknown] and is allowed through to the general-source path, as it
 * always has been. Keeping the two apart is what lets callers drop the raw integer: an
 * unstubbed `Intent.getIntExtra` returns `0`, which must stay a permitted launch.
 */
internal fun Int.toVideoPlayerLaunchSourceType(): VideoPlayerLaunchSourceType =
    when (this) {
        INVALID_VALUE -> VideoPlayerLaunchSourceType.Invalid
        OFFLINE_ADAPTER -> VideoPlayerLaunchSourceType.Offline
        ZIP_ADAPTER -> VideoPlayerLaunchSourceType.Zip
        FILE_BROWSER_ADAPTER -> VideoPlayerLaunchSourceType.FileBrowser
        RUBBISH_BIN_ADAPTER -> VideoPlayerLaunchSourceType.RubbishBin
        BACKUPS_ADAPTER -> VideoPlayerLaunchSourceType.Backups
        LINKS_ADAPTER -> VideoPlayerLaunchSourceType.Links
        INCOMING_SHARES_ADAPTER -> VideoPlayerLaunchSourceType.IncomingShares
        OUTGOING_SHARES_ADAPTER -> VideoPlayerLaunchSourceType.OutgoingShares
        FAVOURITES_ADAPTER -> VideoPlayerLaunchSourceType.Favourites
        CONTACT_FILE_ADAPTER -> VideoPlayerLaunchSourceType.ContactFile
        FOLDER_LINK_ADAPTER -> VideoPlayerLaunchSourceType.FolderLink
        FILE_LINK_ADAPTER -> VideoPlayerLaunchSourceType.FileLink
        FROM_CHAT -> VideoPlayerLaunchSourceType.Chat
        FROM_IMAGE_VIEWER -> VideoPlayerLaunchSourceType.ImageViewer
        FROM_MEDIA_DISCOVERY -> VideoPlayerLaunchSourceType.MediaDiscovery
        FROM_ALBUM_SHARING -> VideoPlayerLaunchSourceType.AlbumSharing
        VERSIONS_ADAPTER -> VideoPlayerLaunchSourceType.Versions
        RECENTS_ADAPTER -> VideoPlayerLaunchSourceType.Recents
        RECENTS_BUCKET_ADAPTER -> VideoPlayerLaunchSourceType.RecentsBucket
        SEARCH_BY_ADAPTER -> VideoPlayerLaunchSourceType.Search
        VIDEO_BROWSE_ADAPTER -> VideoPlayerLaunchSourceType.VideoBrowse
        else -> VideoPlayerLaunchSourceType.Unknown
    }

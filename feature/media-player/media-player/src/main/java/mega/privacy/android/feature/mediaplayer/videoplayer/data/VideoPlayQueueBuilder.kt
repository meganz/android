package mega.privacy.android.feature.mediaplayer.videoplayer.data

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.GetLocalLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodeByHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByEmailUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByHandlesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesByParentHandleUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromInSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromOutSharesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesFromPublicLinksUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideoNodesUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetVideosBySearchTypeUseCase
import mega.privacy.android.domain.usecase.node.backup.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateDownloadLocationUseCase
import mega.privacy.android.feature.mediaplayer.videoplayer.data.mapper.VideoNodeToMediaItemMapper
import mega.privacy.android.feature.mediaplayer.videoplayer.data.model.VideoPlayQueue
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSource
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSourceType
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Builds the queue of videos around the one the user asked for.
 *
 * The first emission is the tapped video on its own, so the player has something to start on while
 * the siblings are still being fetched; the last is the whole queue around it. The tapped video
 * carries the same media id throughout, which is the handle, so the player extends its timeline
 * rather than resetting it and interrupting what is already on screen.
 *
 * A launch that is not part of a playlist stops after the first. One more goes out in between when
 * this build started the streaming server, to hand that over before the slow work rather than
 * after it.
 *
 * Lives in the feature module rather than `:app`, unlike the audio player's equivalent. Nothing it
 * needs is app-only once the launch is described by [VideoPlayerLaunchSourceType] instead of the
 * raw adapter ints the audio builder branches on.
 *
 * Covers the sources whose siblings come out of this account's tree. Offline, zip and folder link
 * resolve their files elsewhere and are gathered in AND-25427; until then a video opened from one
 * of them plays on its own, as it does today.
 */
class VideoPlayQueueBuilder @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val getLocalLinkFromMegaApiUseCase: GetLocalLinkFromMegaApiUseCase,
    private val getVideoNodeByHandleUseCase: GetVideoNodeByHandleUseCase,
    private val getVideoNodesUseCase: GetVideoNodesUseCase,
    private val getVideoNodesByParentHandleUseCase: GetVideoNodesByParentHandleUseCase,
    private val getVideoNodesByHandlesUseCase: GetVideoNodesByHandlesUseCase,
    private val getVideoNodesByEmailUseCase: GetVideoNodesByEmailUseCase,
    private val getVideoNodesFromPublicLinksUseCase: GetVideoNodesFromPublicLinksUseCase,
    private val getVideoNodesFromInSharesUseCase: GetVideoNodesFromInSharesUseCase,
    private val getVideoNodesFromOutSharesUseCase: GetVideoNodesFromOutSharesUseCase,
    private val getVideosBySearchTypeUseCase: GetVideosBySearchTypeUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val getBackupsNodeUseCase: GetBackupsNodeUseCase,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val getOrCreateDownloadLocationUseCase: GetOrCreateDownloadLocationUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val videoNodeToMediaItemMapper: VideoNodeToMediaItemMapper,
) {

    operator fun invoke(source: VideoPlayerLaunchSource): Flow<VideoPlayQueue> = flow {
        val contentUri = source.contentUri ?: return@flow
        val firstItem = videoNodeToMediaItemMapper(
            handle = source.handle,
            uri = contentUri,
            displayName = source.fileName,
        )
        emit(VideoPlayQueue(mediaItems = listOf(firstItem), currentIndex = 0))

        // `isPlaylist` says this launch is one of a set; `rebuildPlaylist` says the caller wants
        // the set worked out. Either being false leaves the single item as the whole queue.
        if (!source.isPlaylist || !source.rebuildPlaylist) return@flow

        emitFullQueue(source, firstItem)
    }.flowOn(ioDispatcher)

    private suspend fun FlowCollector<VideoPlayQueue>.emitFullQueue(
        source: VideoPlayerLaunchSource,
        firstItem: MediaItem,
    ) {
        val startedServer = startStreamingServer()
        if (startedServer) {
            // Handed over before the slow part rather than with the finished queue. Resolving the
            // siblings' streams takes seconds on a large folder, and a player closed inside that
            // window would never learn it owns the server, leaving it up for the rest of the
            // process. The player already holds this item, so this emission changes nothing on
            // screen.
            emit(VideoPlayQueue(listOf(firstItem), 0, startedStreamingServer = true))
        }

        val hiddenNodeFilter = readHiddenNodeFilter()
        val built = buildQueue(source, hiddenNodeFilter) ?: return
        val (items, indexOfRequested) = built

        // Nothing came back around the tapped video — every sibling was filtered out, or none
        // resolved. The video the user opened is already playing and stays that way; this says
        // only that it has no queue, which is what the live player concludes too.
        if (items.isEmpty()) return

        // The tapped video has to sit at its own index carrying the media id the player is
        // already on, or the player resets the timeline instead of extending it, interrupting
        // what is on screen. Reusing the item built for the first emission is the simplest way
        // to be sure of that.
        val queue = when (indexOfRequested) {
            null -> listOf(firstItem) + items
            else -> items.toMutableList().also { it[indexOfRequested] = firstItem }
        }
        emit(
            VideoPlayQueue(
                mediaItems = queue,
                currentIndex = indexOfRequested ?: 0,
                startedStreamingServer = startedServer,
            )
        )
    }

    /**
     * The queue for [source], or null when this launch has no siblings to gather — a chat
     * attachment, a file link, a version. A non-null empty list is different: the source had a
     * queue and filtering took everything out of it.
     *
     * The second value is where the requested video landed, or null when it is not among the
     * siblings at all, which happens when it was filtered out or its stream could not be
     * resolved.
     */
    private suspend fun buildQueue(
        source: VideoPlayerLaunchSource,
        filter: HiddenNodeFilter,
    ): Pair<List<MediaItem>, Int?>? {
        val order = source.sortOrder
        val parentHandle = source.parentHandle

        val nodes = when (source.sourceType) {
            VideoPlayerLaunchSourceType.VideoBrowse ->
                getVideoNodesUseCase(order)

            VideoPlayerLaunchSourceType.Links if parentHandle == INVALID_HANDLE ->
                getVideoNodesFromPublicLinksUseCase(order)

            VideoPlayerLaunchSourceType.IncomingShares if parentHandle == INVALID_HANDLE ->
                getVideoNodesFromInSharesUseCase(order)

            VideoPlayerLaunchSourceType.OutgoingShares if parentHandle == INVALID_HANDLE ->
                getVideoNodesFromOutSharesUseCase(lastHandle = INVALID_HANDLE, order = order)

            VideoPlayerLaunchSourceType.ContactFile if parentHandle == INVALID_HANDLE ->
                source.contactEmail?.let { getVideoNodesByEmailUseCase(it) }

            VideoPlayerLaunchSourceType.Recents,
            VideoPlayerLaunchSourceType.RecentsBucket,
                -> source.nodeHandles?.let { getVideoNodesByHandlesUseCase(it) }

            VideoPlayerLaunchSourceType.Search ->
                source.searchedItems?.let { getVideoNodesByHandlesUseCase(it) }

            VideoPlayerLaunchSourceType.AlbumSharing,
            VideoPlayerLaunchSourceType.ImageViewer,
                -> source.nodeHandles?.let { getVideoNodesByHandlesUseCase(it) }

            VideoPlayerLaunchSourceType.MediaDiscovery ->
                getVideosBySearchTypeUseCase(
                    handle = parentHandle,
                    // Whether the discovery that opened this video reached into sub-folders, so
                    // the queue covers the same ground the grid behind it showed.
                    recursive = isSubFolderMediaDiscoveryEnabled(),
                    order = order,
                )

            VideoPlayerLaunchSourceType.FileBrowser,
            VideoPlayerLaunchSourceType.RubbishBin,
            VideoPlayerLaunchSourceType.Backups,
            VideoPlayerLaunchSourceType.Links,
            VideoPlayerLaunchSourceType.IncomingShares,
            VideoPlayerLaunchSourceType.OutgoingShares,
            VideoPlayerLaunchSourceType.ContactFile,
            VideoPlayerLaunchSourceType.Favourites,
                -> parentOf(source)?.let {
                getVideoNodesByParentHandleUseCase(parentHandle = it, order = order)
            }

            // Chat attachments, file links and versions arrive on their own and stay that way.
            // Offline, zip and folder link do too, until AND-25427 gathers their siblings.
            else -> null
        } ?: return null

        return buildFromNodes(nodes, source, filter)
    }

    /** The folder whose children make up the queue, resolving the roots the launch leaves out. */
    private suspend fun parentOf(source: VideoPlayerLaunchSource): Long? =
        if (source.parentHandle == INVALID_HANDLE) {
            when (source.sourceType) {
                VideoPlayerLaunchSourceType.RubbishBin -> getRubbishNodeUseCase()
                VideoPlayerLaunchSourceType.Backups -> getBackupsNodeUseCase()
                else -> getRootNodeUseCase()
            }?.id?.longValue
        } else {
            getVideoNodeByHandleUseCase(source.parentHandle)?.id?.longValue
        }

    /**
     * Turns [nodes] into media items, dropping the ones whose stream cannot be resolved.
     *
     * Resolution per node is a local-file check with a fingerprint over the file's contents, or a
     * round trip to the SDK for a streaming link. Sequentially that runs into minutes on a queue
     * of a thousand, so it is done concurrently and bounded, which is what the live player does
     * and what the audio builder does not.
     */
    private suspend fun buildFromNodes(
        nodes: List<TypedVideoNode>,
        source: VideoPlayerLaunchSource,
        filter: HiddenNodeFilter,
    ): Pair<List<MediaItem>, Int?> = coroutineScope {
        val visible = nodes
            .filter { !it.isTakenDown }
            .filter { filter.allows(it) }

        val semaphore = Semaphore(URI_RESOLUTION_CONCURRENCY)
        val resolved = visible.map { node ->
            async {
                semaphore.withPermit {
                    runCatching { node to resolveUri(node) }
                        .onFailure { Timber.e(it, "Failed to resolve ${node.id.longValue}") }
                        .getOrNull()
                }
            }
        }.awaitAll().filterNotNull().mapNotNull { (node, uri) ->
            uri?.let { node to videoNodeToMediaItemMapper(node, it) }
        }

        val items = resolved.map { it.second }
        val indexOfRequested = resolved
            .indexOfFirst { it.first.id.longValue == source.handle }
            .takeIf { it >= 0 }
        items to indexOfRequested
    }

    private suspend fun resolveUri(node: TypedVideoNode) = getLocalFilePathUseCase(node)
        ?.takeIf { isLocalCopy(node, it) }
        ?.let { File(it).toUri() }
        ?: getLocalLinkFromMegaApiUseCase(node.id.longValue)?.toUri()

    /**
     * Whether the file at [localPath] is this node rather than something that merely shares its
     * name. A file already in the download folder at the right size is taken at its word; anything
     * else has to match on content.
     */
    private suspend fun isLocalCopy(node: TypedFileNode, localPath: String): Boolean {
        if (isInDownloadFolder(node)) return true
        val fingerprint = node.fingerprint ?: return false
        return runCatching { fingerprint == getFingerprintUseCase(localPath) }.getOrDefault(false)
    }

    private suspend fun isInDownloadFolder(node: TypedFileNode): Boolean {
        val location = getOrCreateDownloadLocationUseCase() ?: return false
        val file = File(location, node.name)
        return file.exists() && file.length() == node.size
    }

    private suspend fun isSubFolderMediaDiscoveryEnabled(): Boolean =
        monitorSubFolderMediaDiscoverySettingsUseCase()
            .catch { Timber.e(it, "Failed to read the sub-folder media discovery setting") }
            .firstOrNull() ?: true

    /** Starts the streaming server unless it is already up, reporting whether this build did it. */
    private suspend fun startStreamingServer(): Boolean {
        if (megaApiHttpServerIsRunningUseCase() != 0) return false
        megaApiHttpServerStartUseCase()
        return true
    }

    /**
     * Read once per build rather than collected: the queue is a snapshot, and a setting that
     * changes while it is being assembled would leave half of it filtered one way and half the
     * other.
     *
     * A read that fails hides rather than shows. The point of this filter is to keep content the
     * user marked sensitive out of sight, so a transient failure must not be what puts it back.
     */
    private suspend fun readHiddenNodeFilter(): HiddenNodeFilter {
        val showHidden = monitorShowHiddenItemsUseCase()
            .catch { Timber.e(it, "Failed to read the hidden items setting") }
            .firstOrNull()
        if (showHidden == true) return HiddenNodeFilter.ShowEverything

        // The two nulls below mean different things. No account detail at all is a failed read,
        // which hides; a detail that carries no paid level is a successful read of an account
        // that has nothing to hide by.
        val accountDetail = monitorAccountDetailUseCase()
            .catch { Timber.e(it, "Failed to read the account detail") }
            .firstOrNull()
            ?: return HiddenNodeFilter.HideSensitive
        val accountType = accountDetail.levelDetail?.accountType
            ?: return HiddenNodeFilter.ShowEverything

        // Hiding is a paid feature, and an expired business account loses it along with the rest.
        if (accountType.isPaid != true) return HiddenNodeFilter.ShowEverything
        if (accountType.isBusinessAccount == true) {
            val expired = runCatching { getBusinessStatusUseCase() }.getOrNull() ==
                    BusinessAccountStatus.Expired
            if (expired) return HiddenNodeFilter.ShowEverything
        }
        return HiddenNodeFilter.HideSensitive
    }

    /** Whether sensitive nodes are kept out of the queue. */
    private enum class HiddenNodeFilter {
        ShowEverything,
        HideSensitive,
        ;

        fun allows(node: TypedVideoNode): Boolean = when (this) {
            ShowEverything -> true
            HideSensitive -> !node.isMarkedSensitive && !node.isSensitiveInherited
        }
    }

    private companion object {
        /**
         * Carried over from the live player, where the comment explains it: resolving a thousand
         * nodes one at a time takes minutes, and this keeps the I/O from going the other way.
         */
        const val URI_RESOLUTION_CONCURRENCY = 16
    }
}

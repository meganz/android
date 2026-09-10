package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Favourites
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Sensitivity
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.photos.ListMediaNodesByOffsetUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import javax.inject.Inject

/**
 * Use case for retrieving predefined system albums.
 *
 * This use case builds a list of [MediaAlbum.System] objects representing system-defined media
 * categories. Each album includes a cover node (if available) and uses its configured
 * [SystemAlbum.mediaTimelineFilter] to determine which media items belong to it.
 *
 * The cover is the first matching node for each album's filter, fetched directly through
 * [ListMediaNodesByOffsetUseCase] (one node per album) rather than filtering the full photo set in
 * memory. This use case reactively combines node updates, the hidden-items setting and account
 * details, so system albums update automatically when any of these dependencies change.
 *
 * @property systemAlbums Set of configured system album types.
 * @property listMediaNodesByOffsetUseCase Use case fetching the album cover node for a given filter.
 * @property monitorNodeUpdatesUseCase Use case for monitoring global node updates, the trigger for
 *           re-reading album covers.
 * @property defaultDispatcher Coroutine dispatcher used for background execution.
 * @property monitorShowHiddenItemsUseCase Use case for monitoring hidden items setting.
 * @property monitorAccountDetailUseCase Use case for monitoring account details.
 */
class MonitorMediaSystemAlbumsUseCase @Inject constructor(
    private val systemAlbums: Set<@JvmSuppressWildcards SystemAlbum>,
    private val listMediaNodesByOffsetUseCase: ListMediaNodesByOffsetUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
) {
    /**
     * Retrieves a reactive stream of system-defined media albums, ordered as [systemAlbums].
     *
     * An album's cover is re-read only when a node change arrives that its own filter can see, so
     * favouriting a photo refreshes just the favourite-scoped albums, and edits no filter cares
     * about - tags, description, label, offline availability - refresh nothing. Covers are
     * otherwise reused, so a cover is only guaranteed fresh for the attributes that drive
     * [coverAffectingChanges]; consumers must not rely on its other fields staying up to date.
     *
     * @return Flow of [List<MediaAlbum.System>] that emits updated albums whenever media nodes,
     *         the hidden items setting, or account details change.
     */
    operator fun invoke(): Flow<List<MediaAlbum.System>> {
        val albums = systemAlbums.toList()

        return combine(
            monitorAlbumRevisions(albums),
            monitorSensitivity(),
        ) { revisions, sensitivity -> CoverKeys(revisions, sensitivity) }
            .distinctUntilChanged()
            .runningFold(INITIAL_SNAPSHOT) { previous, keys ->
                resolveAlbumSnapshot(
                    albums = albums,
                    keys = keys,
                    previous = previous
                )
            }
            .drop(1)
            .map { snapshot -> snapshot.albums.filterNotNull() }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)
    }

    private fun monitorAlbumRevisions(albums: List<SystemAlbum>): Flow<List<Int>> =
        monitorNodeUpdatesUseCase()
            .map { update -> update.coverAffectingChanges() }
            .runningFold(List(albums.size) { 0 }) { revisions, changes ->
                revisions.mapIndexed { index, revision ->
                    if (albums[index].isAffectedBy(changes)) {
                        revision + 1
                    } else {
                        revision
                    }
                }
            }

    /**
     * Folder changes are narrowed to those that take a whole media subtree in or out of scope,
     * because the descendants are not necessarily reported individually. A non-media file only
     * matters when renamed, since changing the extension can move it across the media boundary.
     */
    private fun NodeUpdate.coverAffectingChanges(): Set<NodeChanges> =
        changes.flatMap { (node, nodeChanges) ->
            when {
                node !is FileNode -> nodeChanges.filter { it in SUBTREE_SCOPE_CHANGES }
                node.isMediaNode() -> nodeChanges
                else -> nodeChanges.filter { it == NodeChanges.Name }
            }
        }.toSet()

    private fun SystemAlbum.isAffectedBy(changes: Set<NodeChanges>): Boolean =
        changes.any { change ->
            when (change) {
                NodeChanges.Favourite -> mediaTimelineFilter.favourites != Favourites.All
                else -> change in COVER_SCOPE_CHANGES
            }
        }

    private fun FileNode.isMediaNode(): Boolean =
        type is ImageFileTypeInfo || type is VideoFileTypeInfo

    private fun monitorSensitivity(): Flow<Sensitivity> = combine(
        monitorShowHiddenItemsUseCase(),
        monitorAccountDetailUseCase().map { it.levelDetail?.accountType?.isPaid == true },
    ) { showHiddenItems, isPaid ->
        if (isPaid && !showHiddenItems) {
            Sensitivity.HideSensitive
        } else {
            Sensitivity.ShowAll
        }
    }.distinctUntilChanged()

    private suspend fun resolveAlbumSnapshot(
        albums: List<SystemAlbum>,
        keys: CoverKeys,
        previous: AlbumsSnapshot,
    ): AlbumsSnapshot {
        val reusable = previous.keys?.takeIf { it.sensitivity == keys.sensitivity }

        return AlbumsSnapshot(
            keys = keys,
            albums = albums.mapIndexed { index, album ->
                if (reusable?.revisions?.get(index) == keys.revisions[index]) {
                    previous.albums[index]
                } else {
                    album.loadCover(keys.sensitivity)
                }
            },
        )
    }

    private suspend fun SystemAlbum.loadCover(sensitivity: Sensitivity): MediaAlbum.System? {
        val cover = listMediaNodesByOffsetUseCase(
            filter = mediaTimelineFilter.copy(sensitivity = sensitivity),
            section = null,
            order = SortOrder.ORDER_MODIFICATION_DESC,
            maxElements = 1,
            offset = 0,
        ).firstOrNull()

        return if (hideWhenEmpty && cover == null) {
            null
        } else {
            MediaAlbum.System(id = this, cover = cover)
        }
    }

    private data class CoverKeys(
        val revisions: List<Int>,
        val sensitivity: Sensitivity,
    )

    private data class AlbumsSnapshot(
        val keys: CoverKeys?,
        val albums: List<MediaAlbum.System?>,
    )

    private companion object {
        val INITIAL_SNAPSHOT = AlbumsSnapshot(keys = null, albums = emptyList())

        // Name is included because the SDK derives the media sub-category (GIF, RAW) from the file
        // extension, so a rename can move a node in or out of an album.
        val COVER_SCOPE_CHANGES = setOf(
            NodeChanges.New,
            NodeChanges.Remove,
            NodeChanges.Parent,
            NodeChanges.Sensitive,
            NodeChanges.Timestamp,
            NodeChanges.Name,
        )

        // Narrower than COVER_SCOPE_CHANGES: only these move a folder's whole subtree in or out of
        // album scope. Renaming or re-timestamping a folder leaves its media where it was.
        val SUBTREE_SCOPE_CHANGES = setOf(
            NodeChanges.New,
            NodeChanges.Remove,
            NodeChanges.Parent,
            NodeChanges.Sensitive,
        )
    }
}

package mega.privacy.android.feature.sharelink.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.usecase.GetAlbumPhotosUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
import mega.privacy.android.domain.usecase.SetShowCopyrightUseCase
import mega.privacy.android.domain.usecase.ShouldShowCopyrightUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.link.SplitLinkAndKeyUseCase
import mega.privacy.android.domain.usecase.media.MonitorUserAlbumByIdUseCase
import mega.privacy.android.domain.usecase.node.ExportNodesUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.photos.AlbumHasSensitiveContentUseCase
import mega.privacy.android.domain.usecase.photos.ExportAlbumsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import mega.privacy.android.feature.sharelink.session.ShareLinkPasswordCache
import mega.privacy.android.feature.sharelink.session.ShareLinkSeparateKeyCache
import mega.privacy.android.shared.nodes.extension.getIcon
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import timber.log.Timber
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for the revamped Share link result screen.
 *
 * Serves both subjects the screen can be opened for. For nodes it loads them, ensures each has a
 * public link (batch-exporting the missing ones via [ExportNodesUseCase]) and exposes the account
 * type for the Pro gating of link settings. For an album it loads the album and its photos and
 * exports it via [ExportAlbumsUseCase]. Either way the link is split into its link-without-key and
 * key parts, and the copyright and hidden-items gates apply.
 *
 * [uiState] is lazy and shared with [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed], so the
 * load only starts when the screen begins collecting it.
 */
@HiltViewModel(assistedFactory = ShareLinkViewModel.Factory::class)
class ShareLinkViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val exportNodesUseCase: ExportNodesUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val splitLinkAndKeyUseCase: SplitLinkAndKeyUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val hasSensitiveInheritedUseCase: HasSensitiveInheritedUseCase,
    private val hasSensitiveDescendantUseCase: HasSensitiveDescendantUseCase,
    private val shouldShowCopyrightUseCase: ShouldShowCopyrightUseCase,
    private val setShowCopyrightUseCase: SetShowCopyrightUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorUserAlbumByIdUseCase: MonitorUserAlbumByIdUseCase,
    private val getAlbumPhotosUseCase: GetAlbumPhotosUseCase,
    private val exportAlbumsUseCase: ExportAlbumsUseCase,
    private val albumHasSensitiveContentUseCase: AlbumHasSensitiveContentUseCase,
    private val downloadThumbnailUseCase: DownloadThumbnailUseCase,
    private val passwordCache: ShareLinkPasswordCache,
    private val separateKeyCache: ShareLinkSeparateKeyCache,
) : ViewModel() {

    /**
     * Resume latch for the hidden-items warning: the load suspends here after emitting
     * [ShareLinkUiState.SensitiveWarning] and resumes when the user confirms (true) or cancels
     * (false). Internal plumbing, not UI state.
     */
    private val exportApproval = MutableStateFlow<Boolean?>(null)

    /**
     * Resume latch for the first-time copyright consent: the load suspends here after emitting
     * [ShareLinkUiState.CopyrightConsent] and resumes when the user agrees (true) or declines
     * (false). Internal plumbing, not UI state.
     */
    private val copyrightApproval = MutableStateFlow<Boolean?>(null)

    /**
     * Share link UI state.
     *
     * Lazy so the upstream use cases are only invoked when the screen starts collecting.
     */
    val uiState: StateFlow<ShareLinkUiState> by lazy {
        val accountTypeFlow = monitorAccountDetailUseCase()
            .map { it.levelDetail?.accountType }
            .onStart { emit(null) }
        // Albums support no password, so only a node subject reaches the password cache.
        val nodeHandle = (args.subject as? ShareLinkSubject.Nodes)?.handles?.firstOrNull()
        val passwordFlow = nodeHandle?.let(passwordCache::monitor) ?: flowOf(null)
        val separateKeyFlow = args.subject.cacheKey?.let(separateKeyCache::monitor) ?: flowOf(false)
        combine(
            linkFlow,
            accountTypeFlow,
            passwordFlow,
            separateKeyFlow,
        ) { state, accountType, password, isKeySeparate ->
            if (state !is ShareLinkUiState.Data) return@combine state
            state.copy(
                accountType = accountType,
                isKeySeparate = isKeySeparate,
                isPasswordSet = password != null,
                password = password?.password,
                linkWithPassword = password?.linkWithPassword,
            )
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = ShareLinkUiState.Loading,
        )
    }

    /**
     * Cold flow that loads the subject and its link. Emits [ShareLinkUiState.Loading] first, then
     * [ShareLinkUiState.Data] or [ShareLinkUiState.Error].
     */
    private val linkFlow: Flow<ShareLinkUiState> = flow {
        emit(ShareLinkUiState.Loading)
        val subjectFlow = when (val subject = args.subject) {
            is ShareLinkSubject.Nodes -> nodeLinkFlow(subject.handles)
            is ShareLinkSubject.Album -> albumLinkFlow(subject.albumId)
        }
        emitAll(
            subjectFlow.catch { throwable ->
                Timber.e(throwable, "Failed to load or create the share links")
                emit(ShareLinkUiState.Error)
            }
        )
    }

    /**
     * Loads the nodes and their links. Nodes that already have a public link reuse it; the rest are
     * exported in a single batch via [ExportNodesUseCase].
     */
    private fun nodeLinkFlow(handles: List<Long>): Flow<ShareLinkUiState> = flow {
        if (handles.isEmpty()) {
            emit(ShareLinkUiState.Error)
            return@flow
        }

        val nodes = handles.mapNotNull { handle -> getNodeByIdUseCase(NodeId(handle)) }
        if (nodes.isEmpty()) error("No nodes found for $handles")

        if (!awaitCopyrightConsent()) return@flow
        if (!awaitSensitiveApproval(sensitiveWarningFor(nodes), nodeCount = handles.size)) {
            return@flow
        }

        val pendingHandles = nodes
            .filter { it.exportedData?.publicLink.isNullOrEmpty() }
            .map { it.id.longValue }
        val exportedLinks = if (pendingHandles.isNotEmpty()) {
            exportNodesUseCase(nodes = pendingHandles, callerName = CALLER_NAME)
        } else {
            emptyMap()
        }

        val nodeLinks = nodes.mapNotNull { node ->
            val link = node.exportedData?.publicLink?.takeIf(String::isNotEmpty)
                ?: exportedLinks[node.id.longValue]
                ?: return@mapNotNull null
            val (linkWithoutKey, key) = splitLinkAndKeyUseCase(link)
            val expiryMillis = node.exportedData.expiryMillis()
            ShareLinkNodeItem(
                handle = node.id.longValue,
                name = node.name,
                isFolder = node is FolderNode,
                iconRes = node.getIcon(fileTypeIconMapper),
                sizeInBytes = (node as? FileNode)?.size,
                modificationTime = (node as? FileNode)?.modificationTime,
                childFolderCount = (node as? FolderNode)?.childFolderCount,
                childFileCount = (node as? FolderNode)?.childFileCount,
                link = link,
                linkWithoutKey = linkWithoutKey,
                key = key,
                expirationTime = expiryMillis,
            )
        }

        if (nodeLinks.isEmpty()) {
            emit(ShareLinkUiState.Error)
            return@flow
        }
        emit(ShareLinkUiState.Data(nodeLinks = nodeLinks, accountType = null))
        emitAll(refreshedOnNodeUpdates(nodeLinks))
    }

    /**
     * Loads the album and its link.
     *
     * Once exported, the album and its photos are followed as live flows rather than read once, so
     * a title, cover or photo count changed elsewhere shows here without reopening the screen. The
     * export itself must not repeat, hence the one-shot prologue.
     */
    private fun albumLinkFlow(albumId: Long): Flow<ShareLinkUiState> = flow {
        val id = AlbumId(albumId)

        if (!awaitCopyrightConsent()) return@flow
        val warning = SensitiveWarningType.Items.takeIf { albumHasSensitiveContentUseCase(id) }
        if (!awaitSensitiveApproval(warning, nodeCount = 1)) return@flow

        val link = exportAlbumsUseCase(albumIds = listOf(id))
            .firstOrNull { it.first == id }
            ?.second?.link?.takeIf(String::isNotEmpty)
        if (link == null) {
            emit(ShareLinkUiState.Error)
            return@flow
        }
        val (linkWithoutKey, key) = splitLinkAndKeyUseCase(link)

        emitAll(
            combine(
                monitorUserAlbumByIdUseCase(id).filterNotNull(),
                getAlbumPhotosUseCase(albumId = id, refreshElements = false),
            ) { album, photos -> album to photos }
                .distinctUntilChanged()
                .map { (album, photos) ->
                    ShareLinkUiState.Data(
                        nodeLinks = listOf(
                            ShareLinkNodeItem(
                                handle = albumId,
                                name = album.title,
                                isFolder = false,
                                iconRes = null,
                                sizeInBytes = null,
                                modificationTime = null,
                                childFolderCount = null,
                                childFileCount = null,
                                link = link,
                                linkWithoutKey = linkWithoutKey,
                                key = key,
                            )
                        ),
                        accountType = null,
                        album = ShareLinkAlbumInfo(
                            photoCount = photos.size,
                            coverThumbnailPath = coverThumbnailPath(album.cover),
                        ),
                    )
                }
        )
    }

    /**
     * Runs the first-time copyright gate, returning false when the user declined and the load must
     * be abandoned.
     */
    private suspend fun FlowCollector<ShareLinkUiState>.awaitCopyrightConsent(): Boolean {
        if (!shouldShowCopyrightUseCase()) return true
        emit(ShareLinkUiState.CopyrightConsent)
        if (!copyrightApproval.filterNotNull().first()) return false
        setShowCopyrightUseCase(false)
        return true
    }

    /**
     * Runs the hidden-items gate for the given [warning], returning false when the user cancelled
     * and the load must be abandoned. A null [warning] means there is nothing to warn about.
     */
    private suspend fun FlowCollector<ShareLinkUiState>.awaitSensitiveApproval(
        warning: SensitiveWarningType?,
        nodeCount: Int,
    ): Boolean {
        if (warning == null) return true
        emit(ShareLinkUiState.SensitiveWarning(warning, nodeCount))
        return exportApproval.filterNotNull().first()
    }

    /**
     * Local thumbnail path of the album [cover], downloading it when it is not cached yet. Null
     * when the album is empty or the download failed, leaving the header to its placeholder.
     */
    private suspend fun coverThumbnailPath(cover: TypedFileNode?): String? {
        val path = cover?.thumbnailPath ?: return null
        if (File(path).exists()) return path
        runCatching { downloadThumbnailUseCase(cover.id.longValue) }
            .onFailure { Timber.e(it, "Failed to download the album cover thumbnail") }
        return path.takeIf { File(it).exists() }
    }

    /**
     * Re-reads the nodes whenever the SDK reports a change to one of them, so an expiry saved on the
     * Link settings screen — or changed on another client — shows here without reopening the screen.
     *
     * Only node-derived fields refresh: the copyright consent, hidden-items warning and export in
     * [linkFlow] are one-shot and must not run again. The password and separate-key options need no
     * equivalent, as they are session state combined into [uiState] as live flows.
     */
    private fun refreshedOnNodeUpdates(nodeLinks: List<ShareLinkNodeItem>): Flow<ShareLinkUiState> {
        val handles = nodeLinks.mapTo(mutableSetOf()) { it.handle }
        return monitorNodeUpdatesUseCase()
            .filter { update -> update.changes.keys.any { it.id.longValue in handles } }
            .map { nodeLinks.withUpdatedData() }
            .distinctUntilChanged()
            .map { ShareLinkUiState.Data(nodeLinks = it, accountType = null) }
    }

    /**
     * Re-reads each node's expiry, keeping the item unchanged when the node cannot be read so a
     * transient failure never blanks an expiry that is still set.
     */
    private suspend fun List<ShareLinkNodeItem>.withUpdatedData(): List<ShareLinkNodeItem> =
        map { item ->
            val node = runCatching { getNodeByIdUseCase(NodeId(item.handle)) }
                .onFailure { Timber.e(it, "Failed to refresh node ${item.handle}") }
                .getOrNull() ?: return@map item
            item.copy(
                expirationTime = node.exportedData.expiryMillis(),
                sizeInBytes = (node as? FileNode)?.size,
                modificationTime = (node as? FileNode)?.modificationTime,
                childFolderCount = (node as? FolderNode)?.childFolderCount,
                childFileCount = (node as? FolderNode)?.childFileCount,
            )
        }

    /** The link expiry as an instant, or null when the link never expires. */
    private fun ExportedData?.expiryMillis(): Long? =
        this?.expirationTime?.takeIf { it > 0 }?.seconds?.inWholeMilliseconds

    /**
     * Determines whether the [nodes] about to be exported need a hidden/sensitive-items warning.
     * Mirrors the legacy get-link check: an already-exported node is skipped; a node that is itself
     * hidden (or inherits hidden) triggers [SensitiveWarningType.Items] (which wins), and a folder
     * with hidden descendants triggers [SensitiveWarningType.Folder].
     */
    private suspend fun sensitiveWarningFor(nodes: List<TypedNode>): SensitiveWarningType? {
        var warning: SensitiveWarningType? = null
        for (node in nodes) {
            if (node.exportedData != null) continue
            when {
                node.isMarkedSensitive || hasSensitiveInheritedUseCase(node.id) ->
                    return SensitiveWarningType.Items

                node is FolderNode && hasSensitiveDescendantUseCase(node.id) ->
                    warning = SensitiveWarningType.Folder
            }
        }
        return warning
    }

    /**
     * Confirms the hidden-items warning; the held export proceeds.
     */
    fun onSensitiveWarningConfirmed() {
        exportApproval.value = true
    }

    /**
     * Dismisses the hidden-items warning; the export is abandoned and the screen navigates back.
     */
    fun onSensitiveWarningDismissed() {
        exportApproval.value = false
    }

    /**
     * Agrees to the first-time copyright consent; the held load proceeds and acceptance is
     * persisted so the prompt is not shown again.
     */
    fun onCopyrightAgreed() {
        copyrightApproval.value = true
    }

    /**
     * Declines the first-time copyright consent; the load is abandoned and the screen navigates
     * back.
     */
    fun onCopyrightDisagreed() {
        copyrightApproval.value = false
    }

    /**
     * Assisted factory arguments.
     *
     * @property subject What is being shared: node handles or an album.
     */
    data class Args(val subject: ShareLinkSubject)

    /**
     * Assisted factory for [ShareLinkViewModel].
     */
    @AssistedFactory
    interface Factory {
        /**
         * Create a [ShareLinkViewModel] for the given [args].
         */
        fun create(args: Args): ShareLinkViewModel
    }

    private companion object {
        const val CALLER_NAME = "ShareLinkViewModel"
    }
}

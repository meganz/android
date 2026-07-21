package mega.privacy.android.feature.sharelink.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.feature.sharelink.session.ShareLinkPasswordCache
import mega.privacy.android.feature.sharelink.session.ShareLinkSeparateKeyCache
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.link.SplitLinkAndKeyUseCase
import mega.privacy.android.domain.usecase.node.ExportNodesUseCase
import mega.privacy.android.shared.nodes.extension.getIcon
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import timber.log.Timber

/**
 * ViewModel for the revamped Share link result screen.
 *
 * Loads the shared nodes, ensures each has a public link (batch-exporting the missing ones via
 * [ExportNodesUseCase]), splits every link into its link-without-key and key parts, and exposes the
 * account type for the later Pro gating of link settings.
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
    private val passwordCache: ShareLinkPasswordCache,
    private val separateKeyCache: ShareLinkSeparateKeyCache,
) : ViewModel() {

    /**
     * Share link UI state.
     *
     * Lazy so the upstream use cases are only invoked when the screen starts collecting.
     */
    val uiState: StateFlow<ShareLinkUiState> by lazy {
        val accountTypeFlow = monitorAccountDetailUseCase()
            .map { it.levelDetail?.accountType }
            .onStart { emit(null) }
        val handle = args.handles.firstOrNull()
        val passwordFlow = handle?.let(passwordCache::monitor) ?: flowOf(null)
        val separateKeyFlow = handle?.let(separateKeyCache::monitor) ?: flowOf(false)
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
     * Cold flow that loads the nodes and their links. Emits [ShareLinkUiState.Loading] first, then
     * [ShareLinkUiState.Data] or [ShareLinkUiState.Error].
     *
     * Nodes that already have a public link reuse it; the rest are exported in a single batch via
     * [ExportNodesUseCase].
     */
    private val linkFlow: Flow<ShareLinkUiState> = flow {
        emit(ShareLinkUiState.Loading)

        if (args.handles.isEmpty()) {
            emit(ShareLinkUiState.Error)
            return@flow
        }

        runCatching {
            val nodes = args.handles.mapNotNull { handle -> getNodeByIdUseCase(NodeId(handle)) }
            if (nodes.isEmpty()) error("No nodes found for ${args.handles}")

            val pendingHandles = nodes
                .filter { it.exportedData?.publicLink.isNullOrEmpty() }
                .map { it.id.longValue }
            val exportedLinks = if (pendingHandles.isNotEmpty()) {
                exportNodesUseCase(nodes = pendingHandles, callerName = CALLER_NAME)
            } else {
                emptyMap()
            }

            nodes.mapNotNull { node ->
                val link = node.exportedData?.publicLink?.takeIf(String::isNotEmpty)
                    ?: exportedLinks[node.id.longValue]
                    ?: return@mapNotNull null
                val (linkWithoutKey, key) = splitLinkAndKeyUseCase(link)
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
                )
            }
        }.onSuccess { nodeLinks ->
            if (nodeLinks.isEmpty()) {
                emit(ShareLinkUiState.Error)
            } else {
                emit(ShareLinkUiState.Data(nodeLinks = nodeLinks, accountType = null))
            }
        }.onFailure { throwable ->
            Timber.e(throwable, "Failed to load or create the share links")
            emit(ShareLinkUiState.Error)
        }
    }

    /**
     * Assisted factory arguments.
     *
     * @property handles Node handles whose link is being shared.
     */
    data class Args(val handles: List<Long>)

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

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
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.link.SplitLinkAndKeyUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.shared.nodes.extension.getIcon
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import timber.log.Timber

/**
 * ViewModel for the revamped Share link result screen (single node).
 *
 * Loads the node, ensures it has a public link (creating one via [ExportNodeUseCase] if needed),
 * splits the link into its link-without-key and key parts, and exposes the account type for the
 * later Pro gating of link settings.
 *
 * [uiState] is lazy and shared with [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed], so the
 * load only starts when the screen begins collecting it.
 */
@HiltViewModel(assistedFactory = ShareLinkViewModel.Factory::class)
class ShareLinkViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val exportNodeUseCase: ExportNodeUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val splitLinkAndKeyUseCase: SplitLinkAndKeyUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val passwordCache: ShareLinkPasswordCache,
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
        val passwordFlow = args.handles.firstOrNull()?.let(passwordCache::monitor) ?: flowOf(null)
        combine(linkFlow, accountTypeFlow, passwordFlow) { state, accountType, password ->
            if (state !is ShareLinkUiState.Data) return@combine state
            state.copy(
                accountType = accountType,
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
     * Cold flow that loads the node and its link. Emits [ShareLinkUiState.Loading] first, then
     * [ShareLinkUiState.Data] or [ShareLinkUiState.Error].
     */
    private val linkFlow: Flow<ShareLinkUiState> = flow {
        emit(ShareLinkUiState.Loading)

        val handle = args.handles.firstOrNull()
        if (handle == null) {
            emit(ShareLinkUiState.Error)
            return@flow
        }

        runCatching {
            val node = getNodeByIdUseCase(NodeId(handle))
                ?: error("Node $handle not found")
            val link = node.exportedData?.publicLink?.takeIf(String::isNotEmpty)
                ?: exportNodeUseCase(nodeToExport = NodeId(handle), callerName = CALLER_NAME)
            node to link
        }.onSuccess { (node, link) ->
            val (linkWithoutKey, key) = splitLinkAndKeyUseCase(link)
            emit(
                ShareLinkUiState.Data(
                    handles = args.handles,
                    nodeName = node.name,
                    isFolder = node is FolderNode,
                    iconRes = node.getIcon(fileTypeIconMapper),
                    sizeInBytes = (node as? FileNode)?.size,
                    modificationTime = (node as? FileNode)?.modificationTime,
                    link = link,
                    linkWithoutKey = linkWithoutKey,
                    key = key,
                    accountType = null,
                )
            )
        }.onFailure { throwable ->
            Timber.e(throwable, "Failed to load or create the share link")
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

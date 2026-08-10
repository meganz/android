package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import mega.privacy.android.domain.usecase.node.GetCurrentVersionNodeUseCase
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.mobile.home.presentation.continuewhereleftoff.ContinueWhereLeftOffNameResolver
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ContinueWhereLeftOffViewModel @Inject constructor(
    private val monitorContinueWhereLeftOffItemsUseCase: MonitorContinueWhereLeftOffItemsUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getCurrentVersionNodeUseCase: GetCurrentVersionNodeUseCase,
    private val nameResolver: ContinueWhereLeftOffNameResolver,
) : ViewModel() {

    private val openNodeEventChannel =
        Channel<StateEventWithContent<TypedFileNode>>(Channel.BUFFERED)

    val uiState: StateFlow<ContinueWhereLeftOffUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorContinueWhereLeftOffItemsUseCase(
                limit = MAX_CAROUSEL_ITEMS,
                sortField = ContinueWhereLeftOffSortField.Timestamp,
                sortDirection = SortDirection.Descending,
            )
                .transformLatest { result ->
                    emit(result.copy(items = nameResolver.applyCachedNames(result.items)))
                    if (nameResolver.resolveBlankNames(result.items)) {
                        emit(result.copy(items = nameResolver.applyCachedNames(result.items)))
                    }
                },
            openNodeEventChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
        ) { result, openNodeEvent ->
            ContinueWhereLeftOffUiState(
                items = result.items,
                // Keep the loading skeleton until the hidden-nodes state is resolved so sensitive
                // items are never shown unblurred before their blur is applied (AND-24162).
                isLoading = !result.isHiddenResolved,
                openNodeEvent = openNodeEvent,
            )
        }.catch { e ->
            Timber.e(e, "Failed to load CWLO items")
        }.asUiStateFlow(
            viewModelScope,
            ContinueWhereLeftOffUiState(),
        )
    }

    fun onItemClicked(nodeHandle: Long, type: RecentlyUsedType) {
        viewModelScope.launch {
            runCatching {
                // Text files can be edited and saved back, which overwrites the cloud node with a
                // new version (new handle). The stored handle then points at the stale previous
                // version, so resolve the current version before opening to reflect the latest edit.
                if (type == RecentlyUsedType.TextEditor) {
                    getCurrentVersionNodeUseCase(NodeId(nodeHandle))
                } else {
                    getNodeByIdUseCase(NodeId(nodeHandle)) as? TypedFileNode
                }
            }.onSuccess { node ->
                node?.let { openNodeEventChannel.send(triggered(it)) }
            }.onFailure {
                Timber.d(it)
            }
        }
    }

    fun onOpenNodeEventConsumed() {
        openNodeEventChannel.trySend(consumed())
    }

    companion object {
        // The carousel shows at most 8 cards. Fetch one extra so the UI can tell whether there
        // are more than 8 items and, if so, show a "More" tile that opens the full list
        // (T21373295).
        private const val MAX_CAROUSEL_ITEMS = 9
    }
}

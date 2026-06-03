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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.mobile.home.presentation.continuewhereleftoff.ContinueWhereLeftOffNameResolver
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ContinueWhereLeftOffViewModel @Inject constructor(
    private val monitorContinueWhereLeftOffItemsUseCase: MonitorContinueWhereLeftOffItemsUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
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
                .transformLatest { items ->
                    emit(nameResolver.applyCachedNames(items))
                    if (nameResolver.resolveBlankNames(items)) {
                        emit(nameResolver.applyCachedNames(items))
                    }
                },
            openNodeEventChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
        ) { items, openNodeEvent ->
            ContinueWhereLeftOffUiState(
                items = items,
                openNodeEvent = openNodeEvent,
            )
        }.catch { e ->
            Timber.e(e, "Failed to load CWLO items")
        }.asUiStateFlow(
            viewModelScope,
            ContinueWhereLeftOffUiState(),
        )
    }

    fun onItemClicked(nodeHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getNodeByIdUseCase(NodeId(nodeHandle)) as? TypedFileNode
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
        private const val MAX_CAROUSEL_ITEMS = 10
    }
}

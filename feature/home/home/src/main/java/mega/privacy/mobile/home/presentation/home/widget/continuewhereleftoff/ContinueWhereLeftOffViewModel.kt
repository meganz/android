package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import javax.inject.Inject

@HiltViewModel
internal class ContinueWhereLeftOffViewModel @Inject constructor(
    monitorContinueWhereLeftOffItemsUseCase: MonitorContinueWhereLeftOffItemsUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) : ViewModel() {

    val items: StateFlow<List<ContinueWhereLeftOffItem>> =
        monitorContinueWhereLeftOffItemsUseCase(limit = MAX_CAROUSEL_ITEMS)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    suspend fun resolveNode(nodeHandle: Long): TypedFileNode? =
        runCatching {
            getNodeByIdUseCase(NodeId(nodeHandle)) as? TypedFileNode
        }.getOrNull()

    companion object {
        private const val MAX_CAROUSEL_ITEMS = 10
    }
}

package mega.privacy.android.app.presentation.rubbishbin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.rubbishbin.model.RubbishBinState
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import java.util.Stack
import javax.inject.Inject

/**
 * [ViewModel] class associated to RubbishBinFragment
 *
 * @param getRubbishBinChildrenNode [GetRubbishBinChildrenNode] Fetch the rubbish bin nodes
 * @param monitorNodeUpdates Monitor node updates
 */
@HiltViewModel
class RubbishBinViewModel @Inject constructor(
    private val getRubbishBinChildrenNode: GetRubbishBinChildrenNode,
    private val monitorNodeUpdates: MonitorNodeUpdates,
    private val getRubbishBinParentNodeHandle: GetParentNodeHandle
) : ViewModel() {

    /**
     * The RubbishBin UI State
     */
    private val _state = MutableStateFlow(RubbishBinState())

    /**
     * The RubbishBin UI State accessible outside the ViewModel
     */
    val state: StateFlow<RubbishBinState> = _state

    /**
     * Stack to maintain folder navigation clicks
     */
    private val lastPositionStack = Stack<Int>()

    /**
     * Get current nodes when [RubbishBinViewModel] gets created
     *
     * Uses [monitorNodeUpdates] to observe any Node updates
     *
     * A received Node update will refresh the list of nodes
     */
    init {
        viewModelScope.launch {
            refreshNodes()
            monitorNodeUpdates().collect {
                refreshNodes()
            }
        }
    }

    /**
     * Set the current rubbish bin handle to the UI state
     *
     * @param handle the id of the current rubbish bin parent handle to set
     */
    fun setRubbishBinHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(rubbishBinHandle = handle) }
        refreshNodes()
    }

    /**
     * Retrieves the list of Nodes
     * Call the Use Case [getRubbishBinChildrenNode] to retrieve and return the list of Nodes
     *
     * @return a List of Inbox Nodes
     */
    fun refreshNodes() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    nodes = getRubbishBinChildrenNode(_state.value.rubbishBinHandle) ?: emptyList(),
                    parentHandle = getRubbishBinParentNodeHandle(_state.value.rubbishBinHandle))
            }
        }
    }

    /**
     * Handles back click of rubbishBinFragment
     */
    fun onBackPressed() {
        _state.value.parentHandle?.let {
            setRubbishBinHandle(it)
        }
    }

    /**
     * Pop scroll position for previous depth
     *
     * @return last position saved
     */
    fun popLastPositionStack(): Int = lastPositionStack.takeIf { it.isNotEmpty() }?.pop() ?: 0

    /**
     * Push lastPosition to stack
     * @param lastPosition last position to be added to stack
     */
    fun pushPositionOnStack(lastPosition: Int) {
        lastPositionStack.push(lastPosition)
    }
}

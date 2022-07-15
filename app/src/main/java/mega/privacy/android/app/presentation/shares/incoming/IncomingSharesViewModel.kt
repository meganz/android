package mega.privacy.android.app.presentation.shares.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import javax.inject.Inject

/**
 * ViewModel associated to IncomingSharesFragment
 */
@HiltViewModel
class IncomingSharesViewModel @Inject constructor() : ViewModel() {

    /** private UI state */
    private val _state = MutableStateFlow(IncomingSharesState())

    /** public UI state */
    val state: StateFlow<IncomingSharesState> = _state

    /**
     * Set the current incoming parent handle to the UI state
     *
     * @param handle the id of the current incoming parent handle to set
     */
    fun setIncomingParentHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(incomingParentHandle = handle) }
    }

    /**
     * Decrease by 1 the incoming tree depth
     */
    fun decreaseIncomingTreeDepth() = viewModelScope.launch {
        _state.update { it.copy(incomingTreeDepth = it.incomingTreeDepth - 1) }
    }

    /**
     * Increase by 1 the incoming tree depth
     */
    fun increaseIncomingTreeDepth() = viewModelScope.launch {
        _state.update { it.copy(incomingTreeDepth = it.incomingTreeDepth + 1) }
    }

    /**
     * Reset incoming tree depth to initial value
     */
    fun resetIncomingTreeDepth() = viewModelScope.launch {
        _state.update { it.copy(incomingTreeDepth = 0) }
    }

    /**
     * Set incoming tree depth with given value
     *
     * @param depth the tree depth value to set
     */
    fun setIncomingTreeDepth(depth: Int) = viewModelScope.launch {
        _state.update { it.copy(incomingTreeDepth = depth) }
    }

}
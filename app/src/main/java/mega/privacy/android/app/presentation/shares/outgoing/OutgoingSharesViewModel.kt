package mega.privacy.android.app.presentation.shares.outgoing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import javax.inject.Inject

/**
 * ViewModel associated to OutgoingSharesFragment
 */
@HiltViewModel
class OutgoingSharesViewModel @Inject constructor() : ViewModel() {

    /** private UI state */
    private val _state = MutableStateFlow(OutgoingSharesState())

    /** public UI state */
    val state: StateFlow<OutgoingSharesState> = _state

    /**
     * Set the current outgoing parent handle to the UI state
     *
     * @param handle the id of the current outgoing parent handle to set
     */
    fun setOutgoingParentHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(outgoingParentHandle = handle) }
    }

    /**
     * Decrease by 1 the outgoing tree depth
     */
    fun decreaseOutgoingTreeDepth() = viewModelScope.launch {
        _state.update { it.copy(outgoingTreeDepth = it.outgoingTreeDepth - 1) }
    }

    /**
     * Increase by 1 the outgoing tree depth
     */
    fun increaseOutgoingTreeDepth() = viewModelScope.launch {
        _state.update { it.copy(outgoingTreeDepth = it.outgoingTreeDepth + 1) }
    }

    /**
     * Reset outgoing tree depth to initial value
     */
    fun resetOutgoingTreeDepth() = viewModelScope.launch {
        _state.update { it.copy(outgoingTreeDepth = 0) }
    }


}
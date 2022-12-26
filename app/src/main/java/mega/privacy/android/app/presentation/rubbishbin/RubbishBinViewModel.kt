package mega.privacy.android.app.presentation.rubbishbin

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.presentation.rubbishbin.model.RubbishBinState
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * [ViewModel] class associated to RubbishBinFragment
 *
 * @param getRubbishBinChildrenNode Fetch the rubbish bin nodes
 * @param monitorNodeUpdates Monitor node updates
 */
@HiltViewModel
class RubbishBinViewModel @Inject constructor(
    private val getRubbishBinChildrenNode: GetRubbishBinChildrenNode,
    monitorNodeUpdates: MonitorNodeUpdates,
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
     * Update Rubbish Nodes when a node update callback happens
     */
    val updateRubbishBinNodes: LiveData<Event<List<MegaNode>>> =
        monitorNodeUpdates()
            .mapNotNull { getRubbishBinChildrenNode(_state.value.rubbishBinHandle) }
            .map { Event(it) }
            .asLiveData()


    /**
     * Set the current rubbish bin handle to the UI state
     *
     * @param handle the id of the current rubbish bin parent handle to set
     */
    fun setRubbishBinHandle(handle: Long) = viewModelScope.launch {
        _state.update { it.copy(rubbishBinHandle = handle) }
    }
}

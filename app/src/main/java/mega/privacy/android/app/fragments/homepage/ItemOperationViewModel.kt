package mega.privacy.android.app.fragments.homepage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ItemOperationViewModel @Inject constructor() : ViewModel() {

    private val _openItemEvent = MutableLiveData<Event<NodeItem>>()
    val openItemEvent: LiveData<Event<NodeItem>> = _openItemEvent

    private val _openDisputeNodeEvent = MutableStateFlow(Event(NodeItem()))
    val openDisputeNodeEvent: StateFlow<Event<NodeItem>> = _openDisputeNodeEvent

    private val _showNodeItemOptionsEvent = MutableLiveData<Event<NodeItem>>()
    val showNodeItemOptionsEvent: LiveData<Event<NodeItem>> = _showNodeItemOptionsEvent

    fun onItemClick(item: NodeItem) {
        if (item.node?.isTakenDown == true) {
            _openDisputeNodeEvent.value = Event(item)
        } else {
            _openItemEvent.value = Event(item)
        }
    }

    fun showNodeItemOptions(item: NodeItem) {
        _showNodeItemOptionsEvent.value = Event(item)
    }
}
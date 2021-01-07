package mega.privacy.android.app.fragments.homepage

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ItemOperationViewModel @ViewModelInject constructor() : ViewModel() {

    private val _openItemEvent = MutableLiveData<Event<NodeItem>>()
    val openItemEvent: LiveData<Event<NodeItem>> = _openItemEvent

    private val _showNodeItemOptionsEvent = MutableLiveData<Event<NodeItem>>()
    val showNodeItemOptionsEvent: LiveData<Event<NodeItem>> = _showNodeItemOptionsEvent

    fun onItemClick(item: NodeItem) {
        _openItemEvent.value = Event(item)
    }

    fun showNodeItemOptions(item: NodeItem) {
        _showNodeItemOptionsEvent.value = Event(item)
    }
}
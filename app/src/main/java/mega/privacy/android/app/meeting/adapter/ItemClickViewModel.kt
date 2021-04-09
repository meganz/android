package mega.privacy.android.app.meeting.adapter

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mega.privacy.android.app.fragments.homepage.Event

class ItemClickViewModel @ViewModelInject constructor() : ViewModel() {

    private val _clickItemEvent = MutableLiveData<Event<Participant>>()
    val clickItemEvent: LiveData<Event<Participant>> = _clickItemEvent

    fun onItemClick(item: Participant) {
        _clickItemEvent.value = Event(item)
    }
}


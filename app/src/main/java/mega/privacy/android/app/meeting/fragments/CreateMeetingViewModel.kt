package mega.privacy.android.app.meeting.fragments

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import mega.privacy.android.app.MegaApplication

class CreateMeetingViewModel @ViewModelInject constructor(
    private val repository: CreateMeetingRepository
) : ViewModel() {

    val result = MutableLiveData<Boolean>()
    var meetingName: MutableLiveData<String> = MutableLiveData<String>("Joanna's meeting")

    fun initMeetingName(meetName: String) {
        meetingName.value = meetName
    }
}
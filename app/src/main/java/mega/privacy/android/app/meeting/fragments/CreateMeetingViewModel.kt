package mega.privacy.android.app.meeting.fragments

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CREATING_MEETING

class CreateMeetingViewModel @ViewModelInject constructor(
    private val repository: CreateMeetingRepository
) : ViewModel() {

    val result = MutableLiveData<Boolean>()
    var meetingName: MutableLiveData<String> = MutableLiveData<String>()

    fun initMeetingName(meetName: String) {
        meetingName.value = meetName
    }

    fun initRTCAudioManager() {
        MegaApplication.getInstance()
            .createOrUpdateAudioManager(true, AUDIO_MANAGER_CREATING_MEETING)
    }
}
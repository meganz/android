package mega.privacy.android.app.meeting.fragments

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CREATING_MEETING
import mega.privacy.android.app.utils.StringResourcesUtils

class CreateMeetingViewModel @ViewModelInject constructor(
    private val repository: CreateMeetingRepository
) : ViewModel() {

    var meetingName: MutableLiveData<String> = MutableLiveData<String>()

    fun initMeetingName() {
        meetingName.value = StringResourcesUtils.getString(
            R.string.type_meeting_name, repository.getMyFullName()
        )
    }

    fun initRTCAudioManager() {
        MegaApplication.getInstance()
            .createOrUpdateAudioManager(true, AUDIO_MANAGER_CREATING_MEETING)
    }
}
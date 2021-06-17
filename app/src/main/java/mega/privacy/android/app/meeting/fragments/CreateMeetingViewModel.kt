package mega.privacy.android.app.meeting.fragments

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.StringResourcesUtils

class CreateMeetingViewModel @ViewModelInject constructor(
    private val repository: CreateMeetingRepository
) : ViewModel() {

    var meetingName: MutableLiveData<String> = MutableLiveData<String>()

    /**
     * set the default value of meetingName
     */
    fun initMeetingName() {
        meetingName.value = "";
    }

    /**
     * set the default value of hint
     * @return the string of default meetingName
     */
    fun initHintMeetingName(): String {
        return StringResourcesUtils.getString(
            R.string.type_meeting_name, repository.getMyFullName()
        )
    }
}
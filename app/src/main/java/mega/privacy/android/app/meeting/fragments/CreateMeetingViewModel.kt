package mega.privacy.android.app.meeting.fragments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.StringResourcesUtils
import javax.inject.Inject

@HiltViewModel
class CreateMeetingViewModel @Inject constructor(
    private val repository: CreateMeetingRepository
) : ViewModel() {

    var meetingName: MutableLiveData<String> = MutableLiveData<String>()

    /**
     * Sets the default value of meetingName
     */
    fun initMeetingName() {
        meetingName.value = ""
    }

    /**
     * Sets the default value of hint
     *
     * @return the string of default meetingName

     */
    fun initHintMeetingName(): String {
        return StringResourcesUtils.getString(
            R.string.type_meeting_name, repository.getMyFullName()
        )
    }
}
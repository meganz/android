package mega.privacy.android.app.meeting.fragments

import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequestListenerInterface

class CreateMeetingViewModel @ViewModelInject constructor(
    private val repository: CreateMeetingRepository
) : ViewModel() {

    val result = MutableLiveData<Boolean>()
    var meetingName: MutableLiveData<String> = MutableLiveData<String>()

    fun initMeetingName(meetName: String) {
        meetingName.value = meetName
    }
}
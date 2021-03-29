package mega.privacy.android.app.meeting.fragments

import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.BaseListener
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class CreateMeetingViewModel @ViewModelInject constructor() : ViewModel() {
    private val _avatar = MutableLiveData<Bitmap>()
    val result = MutableLiveData<Boolean>()
    val avatar: LiveData<Bitmap> = _avatar

    /**
     * Create Meeting
     */
    fun createMeeting() {
        // Do something to create the meeting
        result.postValue(true)
    }
}
package mega.privacy.android.app.meeting.fragments

import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequestListenerInterface

class CreateMeetingViewModel @ViewModelInject constructor(
    private val repository: CreateMeetingRepository
) : ViewModel() {
    private val _avatar = MutableLiveData<Bitmap>()
    val result = MutableLiveData<Boolean>()
    val avatar: LiveData<Bitmap> = _avatar


    /**
     * Create Meeting
     *
     * @param group Flag to indicate if the chat is a group chat or not
     * @param peers MegaChatPeerList including other users and their privilege level
     * @param listener MegaChatRequestListener to track this request
     */
    fun createMeeting(
        group: Boolean,
        peers: MegaChatPeerList,
        listener: MegaChatRequestListenerInterface
    ) {
        repository.createMeeting(group, peers, listener)
        result.postValue(true)
    }
}
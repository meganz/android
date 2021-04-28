package mega.privacy.android.app.meeting.fragments

import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.meeting.TestTool
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaChatCall
import kotlin.random.Random

class InMeetingViewModel @ViewModelInject constructor(
    private val inMeetingRepository: InMeetingRepository
) : ViewModel() {

    val participants: MutableLiveData<MutableList<Participant>> = MutableLiveData(mutableListOf())

    //TODO test code start
    val frames: MutableLiveData<MutableList<Bitmap>> = MutableLiveData(mutableListOf())

    private val callStatusObserver =
        androidx.lifecycle.Observer<MegaChatCall> {

        }

    fun addParticipant(add: Boolean) {
        if (add) {
            participants.value!!.add(TestTool.testData()[Random.nextInt(TestTool.testData().size)])
        } else {
            if (participants.value!!.size > 2) {
                participants.value!!.removeAt(participants.value!!.size - 1)
            }
        }
        participants.value = participants.value
    }
    //TODO test code end

    fun setCallOnHold(chatId: Long, isHold: Boolean) {
        inMeetingRepository.setCallOnHold(chatId, isHold)
    }

    fun leaveMeeting(chatId: Long) {
        inMeetingRepository.leaveMeeting(chatId)
    }

    init {
        LiveEventBus.get(
            Constants.EVENT_CALL_STATUS_CHANGE,
            MegaChatCall::class.java
        ).observeForever(callStatusObserver)
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(
            Constants.EVENT_CALL_STATUS_CHANGE,
            MegaChatCall::class.java
        ).removeObserver(callStatusObserver)
    }
}
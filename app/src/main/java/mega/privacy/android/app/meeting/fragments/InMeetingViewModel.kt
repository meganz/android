package mega.privacy.android.app.meeting.fragments

import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mega.privacy.android.app.meeting.TestTool
import mega.privacy.android.app.meeting.adapter.Participant
import kotlin.random.Random

class InMeetingViewModel @ViewModelInject constructor(
    private val inMeetingRepository: InMeetingRepository
) : ViewModel() {

    val participants: MutableLiveData<MutableList<Participant>> = MutableLiveData(mutableListOf())

    //TODO test code start
    val frames: MutableLiveData<MutableList<Bitmap>> = MutableLiveData(mutableListOf())

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

    fun setCallOnHold(chatId: Long?, isHold: Boolean) {
        inMeetingRepository.setCallOnHold(chatId, isHold)
    }

    fun leaveMeeting(chatId: Long?) {
        inMeetingRepository.leaveMeeting(chatId)
    }
}
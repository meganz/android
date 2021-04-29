package mega.privacy.android.app.meeting.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EndMeetingBottomSheetDialogViewModel : ViewModel() {

    private val _currentMode = MutableLiveData(ASSIGN_MODE)
    var currentMode: LiveData<Int> = _currentMode

    private val _clickEvent = MutableLiveData<Int>()
    var clickEvent: LiveData<Int> = _clickEvent

    fun leaveMeetingOrAssignModerator() {

        if (_currentMode.value == LEAVE_CHOOSE_MODE) {
            _currentMode.value = ASSIGN_MODE
        } else {
            _clickEvent.value = ASSIGN_MODERATOR
        }

    }

    fun endMeetingForAllOrLeaveAnyway() {
        _clickEvent.value = if (_currentMode.value == LEAVE_CHOOSE_MODE) {
            END_MEETING_FOR_ALL
        } else {
            LEAVE_ANYWAY
        }
    }

    companion object {
        /**
         * Mode code
         */
        const val LEAVE_CHOOSE_MODE = 0
        const val ASSIGN_MODE = 1

        /**
         * Click action code
         */
        const val END_MEETING_FOR_ALL = 0
        const val LEAVE_ANYWAY = 1
        const val ASSIGN_MODERATOR = 2
    }
}
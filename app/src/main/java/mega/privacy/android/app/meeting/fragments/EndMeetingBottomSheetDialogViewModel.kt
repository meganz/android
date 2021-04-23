package mega.privacy.android.app.meeting.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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

class EndMeetingBottomSheetDialogViewModel : ViewModel() {

    private val _currentMode = MutableLiveData<Int>()
    var currentMode: LiveData<Int> = _currentMode

    private val _clickEvent = MutableLiveData<Int>()
    var clickEvent: LiveData<Int> = _clickEvent

    /**
     * For now, only have assign mode, but will keep the code prevent the UI changing
     */
    init {
        _currentMode.postValue(ASSIGN_MODE)
    }

    fun leaveMeetingOrAssignModerator() {
        if (_currentMode.value == LEAVE_CHOOSE_MODE) {
            _currentMode.postValue(ASSIGN_MODE)
        } else {
            // Open assign moderator page
            _clickEvent.postValue(ASSIGN_MODERATOR)
        }
    }

    fun endMeetingForAllOrLeaveAnyway() {
        if (_currentMode.value == LEAVE_CHOOSE_MODE) {
            // Pop up dialog
            _clickEvent.postValue(END_MEETING_FOR_ALL)
        } else {
            // Leave anyway
            _clickEvent.postValue(LEAVE_ANYWAY)
        }
    }
}
package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingInfoState
import javax.inject.Inject

/**
 * RecurringMeetingInfoActivity view model.

 * @property state    Current view state as [RecurringMeetingInfoState]
 */
@HiltViewModel
class RecurringMeetingInfoViewModel @Inject constructor(
) : ViewModel() {
    private val _state = MutableStateFlow(RecurringMeetingInfoState())
    val state: StateFlow<RecurringMeetingInfoState> = _state
}
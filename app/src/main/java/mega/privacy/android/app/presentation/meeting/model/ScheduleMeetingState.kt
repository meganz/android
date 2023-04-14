package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.ScheduleMeetingViewModel
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import java.util.TimeZone

/**
 * Data class defining the state of [ScheduleMeetingViewModel]
 *
 * @property meetingName                            Meeting name
 * @property freq                                   [OccurrenceFrequencyType].
 * @property startDate                              Start Date.
 * @property endDate                                End Date.
 * @property participantItemList                    List of participants handles.
 * @property finish                                 True, if the activity is to be terminated.
 * @property buttons                                List of available action buttons.
 * @property snackBar                               String resource id for showing an snackBar.
 * @property enabledMeetingLinkOption               True if is enabled the meeting link option, false otherwise.
 * @property discardMeetingDialog                   True if show discard meeting alert dialog, false if not.
 * @property enabledAllowAddParticipantsOption      True if is enabled the allow non-hosts to add participants option, false otherwise.
 * @property addParticipantsNoContactsDialog        True if show add participants no contacts dialog, false if not.
 * @property openAddContact                         True, if should open Add contact screen. False, if not.
 * @property numOfParticipants                      Number of participants.
 */
data class ScheduleMeetingState(
    val meetingName: String? = null,
    val freq: OccurrenceFrequencyType = OccurrenceFrequencyType.Invalid,
    val startDate: TimeZone? = null,
    val endDate: TimeZone? = null,
    val participantItemList: List<Long> = emptyList(),
    val finish: Boolean = false,
    val buttons: List<ScheduleMeetingAction> = ScheduleMeetingAction.values().asList(),
    val snackBar: Int? = null,
    val enabledMeetingLinkOption: Boolean = false,
    val discardMeetingDialog: Boolean = false,
    val enabledAllowAddParticipantsOption: Boolean = true,
    val addParticipantsNoContactsDialog: Boolean = false,
    val openAddContact: Boolean? = null,
    val numOfParticipants: Int = 1,
) {
    /**
     * Get start meeting time
     *
     * @return  start time
     */
    fun getStartTime(): String {
        return "10:00"
    }

    /**
     * Get start meeting date
     *
     * @return  start date
     */
    fun getStartDate(): String {
        return "Fri, 5 Aug, 2022"
    }

    /**
     * Get end meeting time
     *
     * @return  end time
     */
    fun getEndTime(): String {
        return "11:00"
    }

    /**
     * Get end meeting date
     *
     * @return  end date
     */
    fun getEndDate(): String {
        return "Fri, 5 Aug, 2022"
    }
}
package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.ScheduleMeetingViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Data class defining the state of [ScheduleMeetingViewModel]
 *
 * @property openAddContact                         True, if should open Add contact screen. False, if not.
 * @property openChatRoom                           Chat Id of chat room should be opened.
 * @property meetingTitle                           Meeting title
 * @property freq                                   [OccurrenceFrequencyType].
 * @property startDate                              Start Date.
 * @property endDate                                End Date.
 * @property participantItemList                    List of participants handles.
 * @property buttons                                List of available action buttons.
 * @property snackBar                               String resource id for showing an snackBar.
 * @property enabledMeetingLinkOption               True if is enabled the meeting link option, false otherwise.
 * @property discardMeetingDialog                   True if show discard meeting alert dialog, false if not.
 * @property enabledAllowAddParticipantsOption      True if is enabled the allow non-hosts to add participants option, false otherwise.
 * @property addParticipantsNoContactsDialog        True if show add participants no contacts dialog, false if not.
 * @property numOfParticipants                      Number of participants.
 * @property isEditingDescription                   True, if is editing description. False, if not.
 * @property descriptionText                        Description text
 * @property isEmptyTitleError                      True, if an attempt has been made to create a meeting without a title. False, if not.
 */
data class ScheduleMeetingState constructor(
    val openAddContact: Boolean? = null,
    val openChatRoom: Long? = null,
    val meetingTitle: String = "",
    val freq: OccurrenceFrequencyType = OccurrenceFrequencyType.Invalid,
    val startDate: Instant = Instant.now(),
    val endDate: Instant = Instant.now().plus(1, ChronoUnit.HOURS),
    val participantItemList: List<Long> = emptyList(),
    val enabledMeetingLinkOption: Boolean = false,
    val enabledAllowAddParticipantsOption: Boolean = true,
    val descriptionText: String = "",
    val buttons: List<ScheduleMeetingAction> = ScheduleMeetingAction.values().asList(),
    val snackBar: Int? = null,
    val discardMeetingDialog: Boolean = false,
    val addParticipantsNoContactsDialog: Boolean = false,
    val numOfParticipants: Int = 1,
    val isEditingDescription: Boolean = false,
    val isEmptyTitleError: Boolean = false,
) {
    /**
     * Check if it's valid title
     */
    fun isValidMeetingTitle(): Boolean =
        meetingTitle.isNotBlank() && isMeetingTitleTooLong()

    /**
     * Check if meeting title has the right length
     */
    fun isMeetingTitleTooLong(): Boolean = meetingTitle.length <= Constants.MAX_TITLE_SIZE
}

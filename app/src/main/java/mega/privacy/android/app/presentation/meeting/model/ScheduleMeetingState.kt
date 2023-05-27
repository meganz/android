package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.ScheduleMeetingViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.contacts.ContactItem
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Data class defining the state of [ScheduleMeetingViewModel]
 *
 * @property openAddContact                         True, if should open Add contact screen. False, if not.
 * @property chatIdToOpenInfoScreen                 Chat id to open the scheduled meeting info screen.
 * @property meetingTitle                           Meeting title.
 * @property startDate                              Start Date.
 * @property endDate                                End Date.
 * @property participantItemList                    List of participants handles.
 * @property enabledAllowAddParticipantsOption      True if is enabled the allow non-hosts to add participants option, false otherwise.
 * @property enabledMeetingLinkOption               True if is enabled the meeting link option, false otherwise.
 * @property enabledSendCalendarInviteOption       True if is enabled the send calendar invite option, false otherwise.
 * @property descriptionText                        Description text
 * @property buttons                                List of available action buttons.
 * @property snackBar                               String resource id for showing an snackBar.
 * @property discardMeetingDialog                   True if show discard meeting alert dialog, false if not.
 * @property addParticipantsNoContactsDialog        True if show add participants no contacts dialog, false if not.
 * @property numOfParticipants                      Number of participants.
 * @property isEditingDescription                   True, if is editing description. False, if not.
 * @property descriptionText                        Description text.
 * @property isEmptyTitleError                      True, if an attempt has been made to create a meeting without a title. False, if not.
 * @property allowAddParticipants                   True, if can add participants. False, if not.
 * @property recurringMeetingDialog                 True if show recurring meeting dialog, false if not.
 * @property recurringMeetingOptionSelected         [RecurringMeetingType] current option selected.
 * @property scheduledMeetingEnabled                True if the flag feature schedule meeting is enabled. False, if not.
 * @property rulesSelected                          [ChatScheduledRules] selected.
 * @property showMonthlyRecurrenceWarning           True, if the text on the monthly recurrence warning should be displayed. False, if not.
 * @property isCreatingMeeting                      True, if the meeting is being created. False, if not.
 */
data class ScheduleMeetingState constructor(
    val openAddContact: Boolean? = null,
    val chatIdToOpenInfoScreen: Long? = null,
    val meetingTitle: String = "",
    val startDate: ZonedDateTime = Instant.now().atZone(ZoneId.systemDefault()),
    val endDate: ZonedDateTime = Instant.now().atZone(ZoneId.systemDefault())
        .plus(30, ChronoUnit.MINUTES),
    val participantItemList: List<ContactItem> = emptyList(),
    val enabledMeetingLinkOption: Boolean = false,
    val enabledAllowAddParticipantsOption: Boolean = true,
    val enabledSendCalendarInviteOption: Boolean = false,
    val descriptionText: String = "",
    val buttons: List<ScheduleMeetingAction> = ScheduleMeetingAction.values().asList(),
    val snackBar: Int? = null,
    val discardMeetingDialog: Boolean = false,
    val addParticipantsNoContactsDialog: Boolean = false,
    val numOfParticipants: Int = 1,
    val isEditingDescription: Boolean = false,
    val isEmptyTitleError: Boolean = false,
    val allowAddParticipants: Boolean = true,
    val recurringMeetingDialog: Boolean = false,
    val recurringMeetingOptionSelected: RecurringMeetingType = RecurringMeetingType.Never,
    val scheduledMeetingEnabled: Boolean = false,
    val rulesSelected: ChatScheduledRules = ChatScheduledRules(),
    val showMonthlyRecurrenceWarning: Boolean = false,
    val isCreatingMeeting: Boolean = false,
) {
    /**
     * Check if it's valid title
     */
    fun isValidMeetingTitle(): Boolean =
        meetingTitle.isNotBlank() && isMeetingTitleRightSize()

    /**
     * Check if meeting title has the right length
     */
    fun isMeetingTitleRightSize(): Boolean = meetingTitle.length <= Constants.MAX_TITLE_SIZE

    /**
     * Check if meeting description has the right length
     */
    fun isMeetingDescriptionTooLong(): Boolean =
        descriptionText.length > Constants.MAX_DESCRIPTION_SIZE

    /**
     * Get list of participants ids
     */
    fun getParticipantsIds(): List<Long> = mutableListOf<Long>().apply {
        participantItemList.forEach {
            add(it.handle)
        }
    }

    /**
     * Get list of participants emails
     */
    fun getParticipantsEmails(): List<String> =
        mutableListOf<String>().apply {
            participantItemList.forEach {
                add(it.email)
            }
        }
}

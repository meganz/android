package mega.privacy.android.app.presentation.meeting.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.extensions.getDateFormatted
import mega.privacy.android.app.presentation.extensions.getEndTimeFormatted
import mega.privacy.android.app.presentation.extensions.getStartTimeFormatted
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Scheduled meeting management state
 * @property finish                             True, if the activity is to be terminated.
 * @property selectedOccurrence                 Current selected [ChatScheduledMeetingOccurr]
 * @property isChatHistoryEmpty                 True if chat history only has management messages or false otherwise
 * @property chatId                             Chat ID of the scheduled meeting
 * @property selectOccurrenceEvent              Select [ChatScheduledMeetingOccurr] event
 * @property chatRoom                           [ChatRoom] of the scheduled meeting
 * @property snackbarMessageContent             State to show snackbar message
 * @property displayDialog                      Indicates if display confirm dialog or not
 * @property enabledMeetingLinkOption           True if is enabled the meeting link option, false otherwise.
 * @property meetingLink                        Meeting link.
 * @property title                              Meeting Title
 * @property cancelOccurrenceTapped             Indicates if cancel occurrence option was tapped
 * @property editOccurrenceTapped               Indicates if edit occurrence option was tapped
 * @property chatRoomItem                       Selected [ChatRoomItem]
 * @property editedOccurrence                   Edited [ChatScheduledMeetingOccurr]
 * @property editedOccurrenceDate               [ZonedDateTime]
 * @property chatRoomItem                       Selected [ChatRoomItem]
 * @property waitingRoomReminder                [WaitingRoomReminders]
 * @property isCallInProgress                   True, if there is a call in progress. False, if not.
 * @property showForceUpdateDialog              True, if the force update dialog should be shown. False, if not.
 * @property subscriptionPlan                           [AccountType]
 * @property isCallUnlimitedProPlanFeatureFlagEnabled   True, if Call Unlimited Pro Plan feature flag enabled. False, otherwise.
 * @property meetingLinkCreated
 * @property myFullName
 * @property meetingLinkAction
 * @constructor Create empty Scheduled meeting management state
 *
 */
data class ScheduledMeetingManagementUiState(
    val finish: Boolean = false,
    val selectedOccurrence: ChatScheduledMeetingOccurr? = null,
    val isChatHistoryEmpty: Boolean? = null,
    val chatId: Long? = null,
    val selectOccurrenceEvent: StateEvent = consumed,
    val chatRoom: ChatRoom? = null,
    val snackbarMessageContent: StateEventWithContent<String> = consumed(),
    val displayDialog: Boolean = false,
    val enabledMeetingLinkOption: Boolean = false,
    val meetingLink: String? = null,
    val title: String? = null,
    val cancelOccurrenceTapped: Boolean = false,
    val editOccurrenceTapped: Boolean = false,
    val chatRoomItem: ChatRoomItem? = null,
    val editedOccurrence: ChatScheduledMeetingOccurr? = null,
    val editedOccurrenceDate: ZonedDateTime? = null,
    val waitingRoomReminder: WaitingRoomReminders = WaitingRoomReminders.Enabled,
    val isCallInProgress: Boolean = false,
    val showForceUpdateDialog: Boolean = false,
    val subscriptionPlan: AccountType = AccountType.UNKNOWN,
    val isCallUnlimitedProPlanFeatureFlagEnabled: Boolean = false,
    val meetingLinkCreated: StateEvent = consumed,
    val myFullName: String = "",
    val meetingLinkAction: StateEventWithContent<ShareLinkOption> = consumed(),
) {

    /**
     * Check if is valid the edition
     *
     * @return  true if its valid, false if not
     */
    fun isEditionValid(): Boolean =
        selectedOccurrence != editedOccurrence

    /**
     * Check if is date edited
     *
     * @return  true if its valid, false if not
     */
    fun isDateEdited(): Boolean =
        selectedOccurrence?.getDateFormatted() != editedOccurrence?.getDateFormatted()

    /**
     * Check if is start time edited
     *
     * @return  true if its valid, false if not
     */
    fun isStartTimeEdited(is24HourFormat: Boolean): Boolean =
        selectedOccurrence?.getStartTimeFormatted(is24HourFormat) != editedOccurrence?.getStartTimeFormatted(
            is24HourFormat
        )

    /**
     * Check if is end time edited
     *
     * @return  true if its valid, false if not
     */
    fun isEndTimeEdited(is24HourFormat: Boolean): Boolean =
        selectedOccurrence?.getEndTimeFormatted(is24HourFormat) != editedOccurrence?.getEndTimeFormatted(
            is24HourFormat
        )

    /**
     * Should show free plan limit warning
     *
     * @param startDate [ZonedDateTime]
     * @param endDate   [ZonedDateTime]
     */
    fun shouldShowFreePlanLimitWarning(
        startDate: ZonedDateTime?,
        endDate: ZonedDateTime?,
    ): Boolean {
        startDate?.let { start ->
            endDate?.let { end ->
                return isCallUnlimitedProPlanFeatureFlagEnabled &&
                        hasFreePlan() && isDurationExceedingOneHour(start, end)
            }
        }

        return false
    }

    /**
     * Check if duration is longer than 60 minutes
     *
     * @param startDate [ZonedDateTime]
     * @param endDate   [ZonedDateTime]
     */
    private fun isDurationExceedingOneHour(startDate: ZonedDateTime, endDate: ZonedDateTime) =
        Duration.between(startDate, endDate).toMinutes() > FREE_PLAN_DURATION_LIMIT_IN_MINUTES


    /**
     * Check user free plan
     *
     * @return True, if has free plan. False, if has pro plan.
     */
    fun hasFreePlan() = subscriptionPlan == AccountType.FREE

    companion object {
        /**
         * Free plan duration limit in minutes
         */
        const val FREE_PLAN_DURATION_LIMIT_IN_MINUTES = 60
    }
}

/**
 * Share link option
 */
enum class ShareLinkOption {
    /**
     * Send link to chat
     */
    SendLinkToChat,

    /**
     * Share link
     */
    ShareLink
}

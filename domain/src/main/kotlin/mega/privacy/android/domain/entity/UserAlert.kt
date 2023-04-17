package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting

/**
 * User alert
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 */
sealed interface UserAlert {
    val id: Long
    val seen: Boolean
    val createdTime: Long
    val isOwnChange: Boolean
}

/**
 * Custom alert
 *
 * @property heading
 */
sealed interface CustomAlert {
    val heading: String?
}

/**
 * Contact alert
 *
 * @property contact
 */
interface ContactAlert {
    val contact: Contact
}

/**
 * Incoming share alert
 *
 * @property nodeId
 */
sealed interface IncomingShareAlert {
    val nodeId: Long?
    val contact: Contact
}

/**
 * Scheduled meeting alert
 *
 * @property chatId
 * @property title
 * @property email
 * @property startDate
 * @property endDate
 * @property isRecurring
 * @property isOccurrence
 * @property scheduledMeeting
 */
sealed interface ScheduledMeetingAlert {
    val chatId: Long
    val title: String
    val email: String?
    val startDate: Long?
    val endDate: Long?
    val isRecurring: Boolean
    val isOccurrence: Boolean
    val scheduledMeeting: ChatScheduledMeeting?
}

/**
 * Scheduled meeting change type
 *
 * @property value
 */
enum class ScheduledMeetingChangeType(val value: Int) {
    /**
     * Title
     */
    Title(0),

    /**
     * Description
     */
    Description(1),

    /**
     * Canceled
     */
    Canceled(2),

    /**
     * Timezone
     */
    Timezone(3),

    /**
     * Start Date
     */
    StartDate(4),

    /**
     * End Date
     */
    EndDate(5),

    /**
     * Rules
     */
    Rules(6),
}

/**
 * Unknown alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 */
data class UnknownAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    val title: String?,
) : UserAlert

/**
 * Incoming pending contact request alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class IncomingPendingContactRequestAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Incoming pending contact cancelled alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class IncomingPendingContactCancelledAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Incoming pending contact reminder alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class IncomingPendingContactReminderAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Contact change deleted you alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class ContactChangeDeletedYouAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Contact change contact established alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class ContactChangeContactEstablishedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Contact change account deleted alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class ContactChangeAccountDeletedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Contact change blocked you alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class ContactChangeBlockedYouAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Updated pending contact incoming ignored alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class UpdatedPendingContactIncomingIgnoredAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Updated pending contact incoming accepted alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class UpdatedPendingContactIncomingAcceptedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Updated pending contact incoming denied alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class UpdatedPendingContactIncomingDeniedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Updated pending contact outgoing accepted alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class UpdatedPendingContactOutgoingAcceptedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * Updated pending contact outgoing denied alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property contact
 */
data class UpdatedPendingContactOutgoingDeniedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val contact: Contact,
) : UserAlert, ContactAlert {}

/**
 * New share alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 * @property contact
 */
data class NewShareAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val nodeId: Long?,
    override val contact: Contact,
) : UserAlert, IncomingShareAlert {}

/**
 * Deleted share alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 * @property nodeName
 * @property contact
 */
data class DeletedShareAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val nodeId: Long?,
    val nodeName: String?,
    override val contact: Contact,
) : UserAlert, IncomingShareAlert {}


/**
 * Removed from share by owner alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 * @property contact
 */
data class RemovedFromShareByOwnerAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val nodeId: Long?,
    override val contact: Contact,
) : UserAlert, IncomingShareAlert {}

/**
 * New shared nodes alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 * @property contact
 * @property folderCount
 * @property fileCount
 * @property childNodes
 */
data class NewSharedNodesAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val nodeId: Long?,
    override val contact: Contact,
    val folderCount: Int,
    val fileCount: Int,
    val childNodes: List<Long>,
) : UserAlert, IncomingShareAlert {}

/**
 * Removed shared nodes alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 * @property contact
 * @property itemCount
 */
data class RemovedSharedNodesAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val nodeId: Long?,
    override val contact: Contact,
    val itemCount: Int,
) : UserAlert, IncomingShareAlert {}

/**
 * Payment succeeded alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property heading
 * @property title
 */
data class PaymentSucceededAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val heading: String?,
    val title: String?,
) : UserAlert, CustomAlert {}

/**
 * Payment failed alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property heading
 * @property title
 */
data class PaymentFailedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val heading: String?,
    val title: String?,
) : UserAlert, CustomAlert {}

/**
 * Payment reminder alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property heading
 * @property title
 */
data class PaymentReminderAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val heading: String?,
    val title: String?,
) : UserAlert, CustomAlert {}

/**
 * Take down alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property heading
 * @property rootNodeId
 * @property name
 * @property path
 */
data class TakeDownAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val heading: String?,
    val rootNodeId: Long?,
    val name: String?,
    val path: String?,
) : UserAlert, CustomAlert {}

/**
 * Take down reinstated alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property heading
 * @property rootNodeId
 * @property name
 * @property path
 */
data class TakeDownReinstatedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val heading: String?,
    val rootNodeId: Long?,
    val name: String?,
    val path: String?,
) : UserAlert, CustomAlert {}

/**
 * New scheduled meeting alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property chatId
 * @property title
 * @property email
 * @property startDate
 * @property endDate
 * @property isRecurring
 * @property isOccurrence
 * @property scheduledMeeting
 */
data class NewScheduledMeetingAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val chatId: Long,
    override val title: String,
    override val email: String?,
    override val startDate: Long?,
    override val endDate: Long?,
    override val isRecurring: Boolean,
    override val isOccurrence: Boolean,
    override val scheduledMeeting: ChatScheduledMeeting?,
) : UserAlert, ScheduledMeetingAlert

/**
 * Deleted scheduled meeting alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property chatId
 * @property title
 * @property email
 * @property startDate
 * @property endDate
 * @property isRecurring
 * @property isOccurrence
 * @property scheduledMeeting
 */
data class DeletedScheduledMeetingAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val chatId: Long,
    override val title: String,
    override val email: String?,
    override val startDate: Long?,
    override val endDate: Long?,
    override val isRecurring: Boolean,
    override val isOccurrence: Boolean,
    override val scheduledMeeting: ChatScheduledMeeting?,
) : UserAlert, ScheduledMeetingAlert

/**
 * Updated scheduled meeting title alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property chatId
 * @property title
 * @property email
 * @property startDate
 * @property oldTitle
 * @property isRecurring
 * @property isOccurrence
 * @property scheduledMeeting
 */
data class UpdatedScheduledMeetingTitleAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val chatId: Long,
    override val title: String,
    override val email: String?,
    override val startDate: Long?,
    override val endDate: Long?,
    override val isRecurring: Boolean,
    override val isOccurrence: Boolean,
    override val scheduledMeeting: ChatScheduledMeeting?,
    val oldTitle: String,
) : UserAlert, ScheduledMeetingAlert

/**
 * Updated scheduled meeting description alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property chatId
 * @property title
 * @property email
 * @property startDate
 * @property endDate
 * @property isRecurring
 * @property isOccurrence
 * @property scheduledMeeting
 */
data class UpdatedScheduledMeetingDescriptionAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val chatId: Long,
    override val title: String,
    override val email: String?,
    override val startDate: Long?,
    override val endDate: Long?,
    override val isRecurring: Boolean,
    override val isOccurrence: Boolean,
    override val scheduledMeeting: ChatScheduledMeeting?,
) : UserAlert, ScheduledMeetingAlert

/**
 * Updated scheduled meeting date time alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property chatId
 * @property title
 * @property email
 * @property startDate
 * @property endDate
 * @property hasDateChanged
 * @property hasTimeChanged
 * @property isRecurring
 * @property isOccurrence
 * @property scheduledMeeting
 */
data class UpdatedScheduledMeetingDateTimeAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val chatId: Long,
    override val title: String,
    override val email: String?,
    override val startDate: Long?,
    override val endDate: Long?,
    override val isRecurring: Boolean,
    override val isOccurrence: Boolean,
    override val scheduledMeeting: ChatScheduledMeeting?,
    val hasDateChanged: Boolean,
    val hasTimeChanged: Boolean,
) : UserAlert, ScheduledMeetingAlert

/**
 * Updated scheduled meeting cancel alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property chatId
 * @property title
 * @property email
 * @property startDate
 * @property endDate
 * @property isRecurring
 * @property isOccurrence
 * @property scheduledMeeting
 */
data class UpdatedScheduledMeetingCancelAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val chatId: Long,
    override val title: String,
    override val email: String?,
    override val startDate: Long?,
    override val endDate: Long?,
    override val isRecurring: Boolean,
    override val isOccurrence: Boolean,
    override val scheduledMeeting: ChatScheduledMeeting?,
) : UserAlert, ScheduledMeetingAlert

/**
 * Updated scheduled meeting fields alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property chatId
 * @property title
 * @property email
 * @property startDate
 * @property endDate
 * @property isRecurring
 * @property isOccurrence
 * @property scheduledMeeting
 */
data class UpdatedScheduledMeetingFieldsAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val chatId: Long,
    override val title: String,
    override val email: String?,
    override val startDate: Long?,
    override val endDate: Long?,
    override val isRecurring: Boolean,
    override val isOccurrence: Boolean,
    override val scheduledMeeting: ChatScheduledMeeting?,
) : UserAlert, ScheduledMeetingAlert

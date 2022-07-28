package mega.privacy.android.domain.entity

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
 * Contact alert
 *
 * @property userId
 * @property email
 * @property contact
 */
interface ContactAlert {
    val userId: Long
    val email: String?
    val contact: Contact?
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
) : UserAlert

/**
 * Incoming pending contact request alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class IncomingPendingContactRequestAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * Incoming pending contact cancelled alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class IncomingPendingContactCancelledAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * Incoming pending contact reminder alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class IncomingPendingContactReminderAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * Contact change deleted you alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class ContactChangeDeletedYouAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * Contact change contact established alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class ContactChangeContactEstablishedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * Contact change account deleted alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class ContactChangeAccountDeletedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * Contact change blocked you alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class ContactChangeBlockedYouAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * Updated pending contact incoming ignored alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class UpdatedPendingContactIncomingIgnoredAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * Updated pending contact incoming accepted alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class UpdatedPendingContactIncomingAcceptedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
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
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * Updated pending contact outgoing accepted alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class UpdatedPendingContactOutgoingAcceptedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * Updated pending contact outgoing denied alert
 *
 * @property id
 * @property seen
 * @property userId
 * @property createdTime
 * @property isOwnChange
 * @property email
 * @property contact
 */
data class UpdatedPendingContactOutgoingDeniedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val userId: Long,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    override val email: String?,
    override val contact: Contact?,
) : UserAlert, ContactAlert {}

/**
 * New share alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 * @property email
 */
data class NewShareAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    val nodeId: Long?,
    val email: String?,
) : UserAlert {}

/**
 * Deleted share alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 */
data class DeletedShareAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    val nodeId: Long?,
) : UserAlert {}

/**
 * New shared nodes alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 * @property folderCount
 * @property fileCount
 * @property childNodes
 */
data class NewSharedNodesAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    val nodeId: Long?,
    val folderCount: Long,
    val fileCount: Long,
    val childNodes: List<Long>,
) : UserAlert {}

/**
 * Removed shared nodes alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 * @property itemCount
 */
data class RemovedSharedNodesAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    val nodeId: Long?,
    val itemCount: Long,
) : UserAlert {}

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
    val heading: String?,
    val title: String?,
) : UserAlert {}

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
    val heading: String?,
    val title: String?,
) : UserAlert {}

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
    val heading: String?,
    val title: String?,
) : UserAlert {}

/**
 * Take down alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 * @property name
 * @property path
 */
data class TakeDownAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    val nodeId: Long?,
    val name: String?,
    val path: String?,
) : UserAlert {}

/**
 * Take down reinstated alert
 *
 * @property id
 * @property seen
 * @property createdTime
 * @property isOwnChange
 * @property nodeId
 * @property name
 * @property path
 */
data class TakeDownReinstatedAlert(
    override val id: Long,
    override val seen: Boolean,
    override val createdTime: Long,
    override val isOwnChange: Boolean,
    val nodeId: Long?,
    val name: String?,
    val path: String?,
) : UserAlert {}


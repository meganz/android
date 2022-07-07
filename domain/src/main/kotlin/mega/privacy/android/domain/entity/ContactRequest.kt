package mega.privacy.android.domain.entity

/**
 * Contact request
 *
 * @property handle
 * @property sourceEmail
 * @property sourceMessage
 * @property targetEmail
 * @property creationTime
 * @property modificationTime
 * @property status
 * @property isOutgoing
 * @property isAutoAccepted
 */
data class ContactRequest(
    val handle: Long,
    val sourceEmail: String,
    val sourceMessage: String?,
    val targetEmail: String,
    val creationTime: Long,
    val modificationTime: Long,
    val status: ContactRequestStatus,
    val isOutgoing: Boolean,
    val isAutoAccepted: Boolean,
)
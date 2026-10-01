package mega.privacy.android.domain.entity.contacts

/**
 * Contact request lists
 *
 * @property incomingContactRequests list of incoming contact requests
 * @property outgoingContactRequests list of outgoing contact requests
 */
data class ContactRequestLists(
    val incomingContactRequests: List<ContactRequest>,
    val outgoingContactRequests: List<ContactRequest>
)
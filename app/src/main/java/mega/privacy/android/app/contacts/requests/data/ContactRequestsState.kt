package mega.privacy.android.app.contacts.requests.data

internal sealed interface ContactRequestsState {
    val hasIncoming: Boolean
    val hasOutgoing: Boolean

    data object Empty : ContactRequestsState {
        override val hasIncoming = false
        override val hasOutgoing = false
    }

    data class Data(
        val items: List<ContactRequestItem>,
        val incoming: List<ContactRequestItem>,
        val outGoing: List<ContactRequestItem>,
    ) : ContactRequestsState {
        override val hasIncoming = incoming.isNotEmpty()
        override val hasOutgoing = outGoing.isNotEmpty()
    }
}
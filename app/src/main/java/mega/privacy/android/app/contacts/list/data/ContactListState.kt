package mega.privacy.android.app.contacts.list.data

/**
 *  UI state for the contact list screen.
 *  @property showForceUpdateDialog True if the force app update dialog should be shown.
 *  @property shouldOpenChatWithId When not NULL then should open chat
 */
data class ContactListState(
    val showForceUpdateDialog: Boolean = false,
    val shouldOpenChatWithId: Long? = null,
)

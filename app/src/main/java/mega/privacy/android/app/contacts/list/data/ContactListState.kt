package mega.privacy.android.app.contacts.list.data

/**
 *  UI state for the contact list screen.
 *  @property showForceUpdateDialog True if the force app update dialog should be shown.
 *  @property shouldOpenChatWithId When not NULL then should open chat
 *  @property contactActionItems List of contact action items
 *  @property shareFilesToChatEmail Email of the contact awaiting a share-files-to-chat selection.
 *  @property navigateToChatOnAttachSuccess True when a successful attach should navigate to the chat.
 */
data class ContactListState(
    val showForceUpdateDialog: Boolean = false,
    val shouldOpenChatWithId: Long? = null,
    val contactActionItems: List<ContactActionItem> = emptyList(),
    val shareFilesToChatEmail: String? = null,
    val navigateToChatOnAttachSuccess: Boolean = false,
)

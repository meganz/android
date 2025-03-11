package mega.privacy.android.app.presentation.startconversation.model

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.startconversation.StartConversationViewModel]
 *
 * @property buttons             List of available action buttons.
 * @property contactItemList     List of [ContactItem].
 * @property emptyViewVisible    True if the empty view is visible, false otherwise.
 * @property searchAvailable     True if the contact list is not empty, false otherwise.
 * @property searchWidgetState   [SearchWidgetState].
 * @property typedSearch         Typed String for searching, empty by default.
 * @property filteredContactList Filtered list of [ContactItem] based on [typedSearch].
 * @property buttonsVisible      True if the action buttons should be visible because there is not a
 *                               search in progress, false otherwise.
 * @property error               String resource id for showing an error.
 * @property result              Handle of the new chat conversation.
 * @property fromChat            True if the screen is opened from chat, false otherwise.
 * @property isNoteToYourselfFeatureFlagEnabled   True if the note to yourself feature flag is enabled, false if not.
 * @property isNoteToSelfChatEmpty True if the chat is empty. False, otherwise.
 * @property isNoteToSelfNew    True if new label should displayed. False, if not.
 */
data class StartConversationState(
    val buttons: List<StartConversationAction> = StartConversationAction.entries.toList(),
    val contactItemList: List<ContactItem> = emptyList(),
    val emptyViewVisible: Boolean = true,
    val searchAvailable: Boolean = false,
    val searchWidgetState: SearchWidgetState = SearchWidgetState.COLLAPSED,
    val typedSearch: String = "",
    val filteredContactList: List<ContactItem>? = null,
    val buttonsVisible: Boolean = true,
    val error: Int? = null,
    val result: Long? = null,
    val fromChat: Boolean = false,
    val isNoteToYourselfFeatureFlagEnabled: Boolean = false,
    val isNoteToSelfChatEmpty: Boolean = true,
    val isNoteToSelfNew: Boolean = false,
)

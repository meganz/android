package mega.privacy.android.feature.contact.attachment.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent
import kotlinx.collections.immutable.ImmutableList

/**
 * Ui state for the contact attachment screen, listing the contacts attached to a chat message.
 */
sealed interface ContactAttachmentUiState {

    /**
     * Loading state shown while the attached contacts are being resolved.
     */
    data object Loading : ContactAttachmentUiState

    /**
     * Loaded state.
     *
     * @property contacts The contacts attached to the message.
     * @property primaryAction The bottom action offered for the whole attachment, or null when
     * there are no contacts to act on.
     * @property messageEvent One-shot event carrying a feedback message to show in a snackbar.
     * @property openChatEvent One-shot event requesting navigation to a chat, carrying the chat id.
     * @property openContactInfoEvent One-shot event requesting navigation to a contact's info
     * screen, carrying the contact's email.
     */
    data class Data(
        val contacts: ImmutableList<ContactAttachmentItem>,
        val primaryAction: ContactAttachmentPrimaryAction?,
        val messageEvent: StateEventWithContent<ContactAttachmentMessage>,
        val openChatEvent: StateEventWithContent<Long>,
        val openContactInfoEvent: StateEventWithContent<String>,
    ) : ContactAttachmentUiState
}

/**
 * A single contact attached to a chat message, as rendered by the screen.
 *
 * @property handle Contact user handle.
 * @property email Contact email.
 * @property name Contact name as stored in the message.
 * @property isMe True when the attached contact is the current user.
 * @property isContact True when the attached user is a visible contact of the current user.
 * @property isVerified True when the contact's credentials are verified.
 * @property avatarColor Color used as the background of the contact's default avatar.
 */
@Stable
data class ContactAttachmentItem(
    val handle: Long,
    val email: String,
    val name: String,
    val isMe: Boolean,
    val isContact: Boolean,
    val isVerified: Boolean,
    val avatarColor: Int,
) {
    /**
     * Name to render for the row, falling back to the email when no name is stored.
     */
    val displayName: String get() = name.ifBlank { email }
}

/**
 * The bottom primary action offered for the attachment as a whole.
 */
enum class ContactAttachmentPrimaryAction {

    /**
     * Invite the attached users that are not yet contacts.
     */
    Invite,

    /**
     * Start a conversation with the attached contacts.
     */
    StartConversation,
}

/**
 * Feedback messages surfaced by the contact attachment screen.
 */
enum class ContactAttachmentMessage {

    /**
     * The invitation(s) were sent.
     */
    ContactsInvited,

    /**
     * The conversation could not be created.
     */
    StartConversationError,

    /**
     * The tapped attached user is not a visible contact.
     */
    UserNotAContact,
}

package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey.KEY

/**
 * Cannot verify contact dialog nav key
 *
 * @property email
 */
@Serializable
data class CannotVerifyContactDialogNavKey(val email: String) : DialogNavKey

/**
 * Add contacts nav key. Opens the MEGA-contacts multi-select picker; the selected
 * contact emails are published as a `List<String>` under [KEY].
 */
@Serializable
data object AddContactsNavKey : NavKey {
    const val KEY: String = "add_contacts"
}

/**
 * Add chat participants nav key. Opens the MEGA-contacts multi-select picker filtered to the
 * contacts that are not already participants of chat [chatId]; the selected contact emails are
 * published as a `List<String>` under [KEY].
 *
 * @property chatId the chat the selected contacts will be added to; used to exclude existing participants.
 */
@Serializable
data class AddChatParticipantsNavKey(val chatId: Long) : NavKey {
    companion object {
        const val KEY: String = "add_chat_participants"
    }
}

/**
 * Add meeting participants nav key. Like [AddChatParticipantsNavKey] but for an in-call/meeting
 * context: the picker additionally surfaces the call user-limit warning. Selected contact emails
 * are published as a `List<String>` under [KEY].
 *
 * @property chatId the meeting chat the selected contacts will be added to; used to exclude existing
 * participants and to monitor the active call for the user-limit warning.
 */
@Serializable
data class AddMeetingParticipantsNavKey(val chatId: Long) : NavKey {
    companion object {
        const val KEY: String = "add_meeting_participants"
    }
}

/** Launches the legacy "Add contacts" activity in "only create group" mode; result published under [KEY]. */
@Serializable
data object CreateGroupChatNavKey : NavKey {
    const val KEY: String = "create_group_chat"

    /**
     * @property emails Selected contact emails.
     * @property title Optional chat room title.
     * @property isEkr Encrypted key rotation enabled (private, cannot be made public).
     * @property isChatLink A public chat link should be created.
     * @property allowAddParticipants Non-host participants may add others.
     */
    @Serializable
    data class NewGroupChatResult(
        val emails: List<String>,
        val title: String?,
        val isEkr: Boolean,
        val isChatLink: Boolean,
        val allowAddParticipants: Boolean,
    )
}

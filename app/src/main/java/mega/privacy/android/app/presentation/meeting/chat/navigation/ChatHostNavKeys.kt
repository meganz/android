package mega.privacy.android.app.presentation.meeting.chat.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param chatId Chat id to open.
 * @param action Chat action (defaults to show messages).
 * @param link Optional chat link when the chat is opened from a link.
 */
@Serializable
internal data class ChatLegacyContainerNavKey(
    val chatId: Long,
    val action: String,
    val link: String? = null,
) : NavKey

/**
 * @param showMeetingTab True to open with the Meetings tab selected.
 * @param createNewChat True to open with the "Create new chat" flow.
 */
@Serializable
internal data class ChatTabsContainerNavKey(
    val showMeetingTab: Boolean = false,
    val createNewChat: Boolean = false,
) : NavKey

package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption

/**
 * Sealed class defining all the possible data for showing some info to the user.
 */
sealed class InfoToShow {
    /**
     * Show simple string.
     *
     * @property stringId String id.
     */
    data class SimpleString(@StringRes val stringId: Int) : InfoToShow()


    /**
     * Show quantity string.
     *
     * @property stringId String id.
     * @property count Quantity.
     */
    data class QuantityString(@PluralsRes val stringId: Int, val count: Int) : InfoToShow()

    /**
     * Show string with params.
     *
     * @property stringId String id.
     * @property args List of strings in case the string admits some params.
     */
    data class StringWithParams(
        @StringRes val stringId: Int,
        val args: List<String> = emptyList(),
    ) : InfoToShow()

    /**
     * Show result of inviting a contact to a chat.
     *
     * @property result [InviteContactToChatResult]
     */
    data class InviteContactResult(
        val result: InviteContactToChatResult,
    ) : InfoToShow()

    /**
     * Show result of muting push notifications.
     *
     * @property result [ChatPushNotificationMuteOption]
     */
    data class MuteOptionResult(
        val result: ChatPushNotificationMuteOption,
    ) : InfoToShow()

    /**
     * Show result of forwarding messages.
     *
     * @property result [ForwardMessagesResult]
     */
    data class ForwardMessagesResult(
        val result: ForwardMessagesToChatsResult,
    ) : InfoToShow()

    /**
     * Show result of inviting a user as a contact.
     *
     * @property result [InviteUserAsContactResultOption]
     */
    data class InviteUserAsContactResult(
        val result: InviteUserAsContactResultOption,
    ) : InfoToShow()
}

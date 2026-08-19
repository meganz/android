package mega.privacy.android.feature.chat.list.model

import androidx.annotation.StringRes

/**
 * Confirmation prompt shown before a destructive chat room action runs.
 *
 * @property title Dialog title string resource.
 * @property message Dialog body string resource.
 * @property confirmButtonText Positive button string resource.
 * @property testTag Test tag applied to the confirmation dialog.
 */
data class ChatRoomMenuItemConfirmation(
    @param:StringRes val title: Int,
    @param:StringRes val message: Int,
    @param:StringRes val confirmButtonText: Int,
    val testTag: String,
)

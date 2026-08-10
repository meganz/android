package mega.privacy.android.app.presentation.chat.list

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * Confirmation state for the "leave chat(s)" dialog.
 *
 * @property chatIds Chat ids to leave when the user confirms.
 * @property titleRes Title to show — group vs meeting variant.
 */
internal data class LeaveConfirmation(
    val chatIds: List<Long>,
    @StringRes val titleRes: Int =
        R.string.title_confirmation_leave_group_chat,
)
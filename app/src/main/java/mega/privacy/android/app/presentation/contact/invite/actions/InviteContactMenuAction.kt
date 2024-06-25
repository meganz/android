package mega.privacy.android.app.presentation.contact.invite.actions

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.invite.InviteContactScreen
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithoutIcon

/**
 * [InviteContactScreen] option menu actions.
 */
sealed interface InviteContactMenuAction : MenuAction {

    /**
     * Action to invite a friend via a generated link.
     */
    data object InviteAFriendViaALink : MenuActionWithoutIcon(
        descriptionRes = R.string.invite_contact_action_button,
        testTag = INVITE_A_FRIEND_ACTION_TAG
    ), InviteContactMenuAction

    /**
     * Action to open the user's QR code.
     */
    data object MyQRCode : MenuActionWithoutIcon(
        descriptionRes = R.string.choose_qr_option_panel,
        testTag = MY_QR_CODE_ACTION_TAG
    ), InviteContactMenuAction

    companion object {
        internal const val INVITE_A_FRIEND_ACTION_TAG =
            "invite_contact_screen:action_invite_a_friend_via_a_generated_link"
        internal const val MY_QR_CODE_ACTION_TAG = "invite_contact_screen:action_open_my_qr_code"
    }
}

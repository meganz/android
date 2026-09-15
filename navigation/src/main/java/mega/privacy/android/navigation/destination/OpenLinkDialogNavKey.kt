package mega.privacy.android.navigation.destination

import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

/**
 * Navigation key for the open link dialog.
 *
 * @param isJoinMeeting True to render the dialog in "join meeting" mode, asking for a meeting link
 * rather than a generic MEGA link.
 */
@Serializable
data class OpenLinkDialogNavKey(
    val isJoinMeeting: Boolean = false,
) : DialogNavKey

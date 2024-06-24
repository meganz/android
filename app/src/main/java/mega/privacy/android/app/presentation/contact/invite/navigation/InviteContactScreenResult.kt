package mega.privacy.android.app.presentation.contact.invite.navigation

/**
 * A model represents the result from this screen. Used for start activity for result.
 *
 * @property key The key for the result.
 * @property totalInvitationsSent The total number of invitations sent.
 */
data class InviteContactScreenResult(
    val key: String,
    val totalInvitationsSent: Int,
) {
    companion object {
        internal const val KEY_SENT_NUMBER = "sentNumber"
    }
}

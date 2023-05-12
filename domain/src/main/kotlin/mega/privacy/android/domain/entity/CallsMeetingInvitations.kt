package mega.privacy.android.domain.entity

/**
 * Enum class defining call meeting invitations settings.
 */
enum class CallsMeetingInvitations {
    /**
     * Meeting invitations enabled
     */
    Enabled,

    /**
     * Meeting invitations disabled
     */
    Disabled;

    companion object {
        val DEFAULT = Enabled
    }
}

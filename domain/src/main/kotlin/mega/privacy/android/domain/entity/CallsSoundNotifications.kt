package mega.privacy.android.domain.entity

/**
 * Enum class defining call sound notifications settings.
 */
enum class CallsSoundNotifications {
    /**
     * Sound notifications enabled
     */
    Enabled,

    /**
     * Sound notifications disabled
     */
    Disabled;

    companion object {
        val DEFAULT = Enabled
    }

}
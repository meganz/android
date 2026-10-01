package mega.privacy.android.domain.entity

/**
 * Enum class defining call meeting reminders settings.
 */
enum class CallsMeetingReminders {
    /**
     * Meeting reminders enabled
     */
    Enabled,

    /**
     * Meeting reminders disabled
     */
    Disabled;

    companion object {
        val DEFAULT = Enabled
    }
}

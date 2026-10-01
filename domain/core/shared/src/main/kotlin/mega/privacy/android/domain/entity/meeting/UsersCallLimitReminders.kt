package mega.privacy.android.domain.entity.meeting

/**
 * Enum class defining users call limit reminders settings.
 */
enum class UsersCallLimitReminders {
    /**
     * Users call limit reminders enabled
     */
    Enabled,

    /**
     * Users call limit reminders disabled
     */
    Disabled;

    companion object {
        /**
         * Default value is enabled
         */
        val DEFAULT = Enabled
    }
}
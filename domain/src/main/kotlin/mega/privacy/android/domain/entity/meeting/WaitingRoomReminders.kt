package mega.privacy.android.domain.entity.meeting

/**
 * Enum class defining waiting room reminders settings.
 */
enum class WaitingRoomReminders {
    /**
     * Waiting room reminders enabled
     */
    Enabled,

    /**
     * Waiting room reminders disabled
     */
    Disabled;

    companion object {
        /**
         * Default value is enabled
         */
        val DEFAULT = Enabled
    }
}
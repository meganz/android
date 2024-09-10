package mega.privacy.android.feature.sync.ui.model

/**
 * Model representing how often should the Sync run
 *
 * @property minutes The frequency in minutes
 */
enum class SyncFrequency(val minutes: Int) {
    EVERY_15_MINUTES(15),
    EVERY_30_MINUTES(30),
    EVERY_45_MINUTES(45),
    EVERY_HOUR(60);

    companion object {
        /**
         * Get the SyncFrequency from the minutes
         */
        fun fromMinutes(minutes: Int) = entries.first { it.minutes == minutes }
    }
}
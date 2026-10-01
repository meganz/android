package mega.privacy.android.domain.entity.meeting

/**
 * Types of frequency of the recurring scheduled meeting
 */
enum class OccurrenceFrequencyType {
    /**
     * Invalid value
     */
    Invalid,

    /**
     * Occurs daily
     */
    Daily,

    /**
     * Occurs weekly
     */
    Weekly,

    /**
     * Occurs monthly
     */
    Monthly
}
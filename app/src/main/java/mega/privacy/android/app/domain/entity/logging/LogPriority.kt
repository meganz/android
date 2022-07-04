package mega.privacy.android.app.domain.entity.logging

/**
 * Log priority
 *
 * An intermediary class to facilitate translating Android log constants to strings for logging.
 * Only used in this fashion in the Chat logs as the Chat SDK concatenates log messages, sources
 * and levels internally forcing us to manually distinguish and apply log level strings
 * where appropriate.
 *
 *
 * @property intValue maps to android logging priority values
 */
enum class LogPriority(val intValue: Int) {
    /**
     * Verbose
     */
    VERBOSE(2),

    /**
     * Debug
     */
    DEBUG(3),

    /**
     * Info
     */
    INFO(4),

    /**
     * Assert
     */
    ASSERT(7),

    /**
     * Warn
     */
    WARN(5),

    /**
     * Error
     */
    ERROR(6),

    /**
     * Unknown
     */
    UNKNOWN(-1);

    companion object {
        fun fromInt(value: Int): LogPriority =
            values().firstOrNull { it.intValue == value } ?: UNKNOWN
    }
}

package mega.privacy.android.feature.chat.managehistory.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

internal val ONE_WEEK = 7.days
internal val ONE_MONTH = 30.days
internal val ONE_YEAR = 365.days

/**
 * How long a chat keeps its messages before deleting them.
 *
 * Every case carries the [duration] it stands for, so the UI never has to translate between a
 * preset and a period. The conversion to the whole seconds the SDK takes happens once, where the
 * use case is called.
 */
sealed interface ChatHistoryRetention {

    /**
     * Period after which messages are deleted. [Duration.ZERO] means messages are kept.
     */
    val duration: Duration

    /**
     * Messages are kept indefinitely.
     */
    data object Disabled : ChatHistoryRetention {
        override val duration = Duration.ZERO
    }

    /**
     * Messages older than one day are deleted.
     */
    data object OneDay : ChatHistoryRetention {
        override val duration = 1.days
    }

    /**
     * Messages older than one week are deleted.
     */
    data object OneWeek : ChatHistoryRetention {
        override val duration = ONE_WEEK
    }

    /**
     * Messages older than one month are deleted.
     */
    data object OneMonth : ChatHistoryRetention {
        override val duration = ONE_MONTH
    }

    /**
     * Messages older than a period the user picked are deleted.
     *
     * @property duration the picked period.
     */
    data class Custom(override val duration: Duration) : ChatHistoryRetention

    companion object {

        /**
         * The retention a [duration] stands for: one of the presets when it matches exactly,
         * [Disabled] when nothing is kept, and [Custom] otherwise.
         */
        fun from(duration: Duration): ChatHistoryRetention = when (duration) {
            Duration.ZERO -> Disabled
            OneDay.duration -> OneDay
            OneWeek.duration -> OneWeek
            OneMonth.duration -> OneMonth
            else -> Custom(duration)
        }
    }
}

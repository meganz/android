package mega.privacy.android.navigation.contract.suppression

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.queue.QueueEvent

/**
 * Suppression type
 */
sealed interface SuppressionType {

    /**
     * Suppresses event
     *
     * @param event
     * @return true if event is suppressed
     */
    fun suppressesEvent(event: QueueEvent): Boolean

    /**
     * None
     */
    data object None : SuppressionType {

        /**
         * Suppresses event
         *
         * @param event
         * @return false
         */
        override fun suppressesEvent(event: QueueEvent): Boolean = false
    }

    /**
     * Complete
     */
    data object Complete : SuppressionType {

        /**
         * Suppresses event
         *
         * @param event
         * @return true if event is suppressable
         */
        override fun suppressesEvent(event: QueueEvent): Boolean = event.suppressableKey != null
    }

    /**
     * With exceptions
     *
     * @property exceptions
     */
    data class WithExceptions(val exceptions: List<NavKey>) : SuppressionType {

        /**
         * Suppresses event
         *
         * @param event
         * @return true if event is suppressable and not in exceptions
         */
        override fun suppressesEvent(event: QueueEvent): Boolean =
            event.suppressableKey?.let { exceptions.contains(it).not() } ?: false
    }
}

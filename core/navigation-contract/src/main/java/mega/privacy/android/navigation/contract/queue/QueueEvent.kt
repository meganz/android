package mega.privacy.android.navigation.contract.queue

import androidx.navigation3.runtime.NavKey

/**
 * Queue event
 *
 * @property suppressableKey Whether the event is suppressable or not (Supressable events will not trigger on screens with OverlaySuppression metadata)
 */
interface QueueEvent {
    val suppressableKey: NavKey?
}
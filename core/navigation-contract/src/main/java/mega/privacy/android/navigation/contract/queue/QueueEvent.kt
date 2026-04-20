package mega.privacy.android.navigation.contract.queue

/**
 * Queue event
 *
 * @property isSuppressable Whether the event is suppressable or not (Supressable events will not trigger on screens with OverlaySuppression metadata)
 */
interface QueueEvent {
    val isSuppressable: Boolean
}
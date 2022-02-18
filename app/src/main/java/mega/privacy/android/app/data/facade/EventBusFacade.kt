package mega.privacy.android.app.data.facade

import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


/**
 * Event bus facade
 *
 * A facade class to decouple calls to the event bus observer. Global pub/sub implementations are
 * generally considered to be a bad design as it can make debugging hard and obscure sources of bugs.
 * It is always better to observe an explicit source of events. Converting these events to a flow
 * via this facade enables us to later refactor the code to be more explicit without having to
 * change the implementation in the consumer.
 *
 * @constructor Create Event bus facade
 */
abstract class EventBusFacade<T> constructor(
    private val event: String,
    private val type: Class<T>
) {

    fun getEvents(): Flow<T> {
        return getEventFlow(event, type)
    }

    /**
     * Get flow of event notifications
     *
     * @param T event type
     * @param event identifier of the event to observe
     * @param type event type
     * @return a flow that emits all events of the requested type
     */
    private fun <T> getEventFlow(
        event: String,
        type: Class<T>
    ): Flow<T> {
        val eventObservable =
            LiveEventBus.get(event, type)

        return callbackFlow {
            val flowObserver = Observer(this::trySend)
            eventObservable.observeForever(flowObserver)
            awaitClose { eventObservable.removeObserver(flowObserver) }
        }
    }
}
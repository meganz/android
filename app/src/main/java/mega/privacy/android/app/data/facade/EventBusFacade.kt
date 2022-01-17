package mega.privacy.android.app.data.facade

import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jeremyliao.liveeventbus.core.Observable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class EventBusFacade @Inject constructor() {
    fun <T> getEventFlow(
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
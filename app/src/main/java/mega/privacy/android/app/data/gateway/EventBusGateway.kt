package mega.privacy.android.app.data.gateway

import com.jeremyliao.liveeventbus.LiveEventBus
import com.jeremyliao.liveeventbus.core.Observable
import javax.inject.Inject

class EventBusGateway @Inject constructor() {
    fun <T> getEventObservable(event: String, type: Class<T>): Observable<T> {
        return LiveEventBus.get(event, type)
    }
}
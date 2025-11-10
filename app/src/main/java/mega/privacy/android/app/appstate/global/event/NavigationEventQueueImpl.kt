package mega.privacy.android.app.appstate.global.event

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationEventQueueImpl @Inject constructor() : NavigationEventQueue,
    NavigationEventQueueReceiver {
    private val _events =
        Channel<List<NavKey>>(
            capacity = 10,
            onUndeliveredElement = { Timber.w("Failed to deliver navigation event: $it") })
    override val events: ReceiveChannel<List<NavKey>> = _events
    override suspend fun emit(navKey: NavKey) {
        _events.send(listOf(navKey))
    }

    override suspend fun emit(navKeys: List<NavKey>) {
        _events.send(navKeys)
    }

    override fun tryEmit(navKey: NavKey) {
        _events.trySend(listOf(navKey))
    }

    override fun tryEmit(navKeys: List<NavKey>) {
        _events.trySend(navKeys)
    }
}
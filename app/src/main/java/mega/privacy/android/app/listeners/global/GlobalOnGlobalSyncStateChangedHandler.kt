package mega.privacy.android.app.listeners.global

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.notifications.NotifyNotificationCountChangeUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class, ObsoleteCoroutinesApi::class)
class GlobalOnGlobalSyncStateChangedHandler @Inject constructor(
    @ApplicationScope private val coroutineScope: CoroutineScope,
    private val notifyNotificationCountChangeUseCase: NotifyNotificationCountChangeUseCase,
) {
    val timeout = 1.seconds

    private val throttleActor = coroutineScope.actor<Unit>(capacity = Channel.CONFLATED) {
        for (msg in channel) {
            notifyNotificationCountChangeUseCase()
            delay(timeout)
        }
    }

    operator fun invoke() {
        coroutineScope.launch { throttleActor.send(Unit) }
    }
}
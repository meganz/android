package mega.privacy.android.app.appstate.global.event

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueueReceiver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnackbarEventQueueImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SnackbarEventQueue, SnackbarEventQueueReceiver {
    private val _events = Channel<SnackbarAttributes>(Channel.UNLIMITED)

    override val eventQueue: ReceiveChannel<SnackbarAttributes> = _events

    override suspend fun queueMessage(message: String) =
        queueMessage(SnackbarAttributes(message))

    override suspend fun queueMessage(resId: Int, vararg args: Any) {
        queueMessage(context.getString(resId, *args))
    }

    override suspend fun queueMessage(attributes: SnackbarAttributes) {
        _events.send(attributes)
    }
}

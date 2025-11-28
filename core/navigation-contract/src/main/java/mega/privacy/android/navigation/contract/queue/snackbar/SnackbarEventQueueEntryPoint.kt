package mega.privacy.android.navigation.contract.queue.snackbar

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.atomic.AtomicReference

/**
 * Extension function to get [SnackbarEventQueue] from the application context.
 *
 * This is useful for classes that are not Hilt components and need access to the snackbar event queue.
 *
 * @return [SnackbarEventQueue] instance.
 */
val Context.snackbarEventQueue: SnackbarEventQueue
    get() = SnackbarEventQueueProvider.get(this)

/**
 * This interface is needed to inject [SnackbarEventQueue] into classes that are not Hilt components.
 *
 * @property snackbarEventQueue The [SnackbarEventQueue] instance.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SnackbarEventQueueEntryPoint {
    val snackbarEventQueue: SnackbarEventQueue
}

internal object SnackbarEventQueueProvider {
    private val queueRef = AtomicReference<SnackbarEventQueue?>(null)

    fun get(context: Context): SnackbarEventQueue {
        return queueRef.get() ?: run {
            val newQueue = EntryPointAccessors.fromApplication(
                context.applicationContext,
                SnackbarEventQueueEntryPoint::class.java
            ).snackbarEventQueue

            queueRef.set(newQueue)
            newQueue
        }
    }
}


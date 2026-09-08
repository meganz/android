package mega.privacy.android.app.service.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.fcm.MegaMessageCoordinator
import timber.log.Timber

/**
 * Gives [MegaMessageService] access to the [MegaMessageCoordinator] that does its work.
 *
 * Resolved per callback rather than injected, so a service creation that finds no dependency
 * graph can be handled instead of being fatal — see [MegaMessageService.coordinatorOrNull].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PushMessageEntryPoint {
    fun megaMessageCoordinator(): MegaMessageCoordinator
}

/**
 * Receives Firebase push callbacks and hands them to [MegaMessageCoordinator]; it holds no logic
 * of its own.
 */
class MegaMessageService : FirebaseMessagingService() {

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("Remote Message: $remoteMessage")
        coordinatorOrNull()?.onMessageReceived(remoteMessage.data)
    }

    override fun onNewToken(token: String) {
        Timber.d("New token: $token")
        coordinatorOrNull()?.onNewToken(token)
    }

    /**
     * The [MegaMessageCoordinator], or null when no Hilt component exists.
     *
     * That only happens under `HiltTestApplication`, where the component is scoped to a single
     * test case: Play services binds this service at an unrelated moment, and injecting in
     * `onCreate` would fail there and kill the app process, aborting the whole instrumentation
     * run. The failure cannot be reported through `CrashReporter` because the graph holding it is
     * what is missing.
     */
    private fun coordinatorOrNull(): MegaMessageCoordinator? = try {
        EntryPointAccessors.fromApplication<PushMessageEntryPoint>(applicationContext)
            .megaMessageCoordinator()
    } catch (e: IllegalStateException) {
        Timber.w(e, "No dependency graph available, dropping push callback")
        null
    }
}

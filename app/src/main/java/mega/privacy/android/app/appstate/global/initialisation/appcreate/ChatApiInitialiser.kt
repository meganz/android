package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.app.globalmanagement.ChatApiListenerCoordinator
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser
import javax.inject.Inject

/**
 * Registers the global chat SDK listeners via [ChatApiListenerCoordinator].
 *
 * Critical: chat request, notification and call listeners must be attached before any chat
 * activity can occur, exactly as `setupMegaChatApi` ran synchronously in `onCreate`.
 */
internal class ChatApiInitialiser @Inject constructor(
    private val chatApiListenerCoordinator: ChatApiListenerCoordinator,
) : SynchronousAppCreateInitialiser {
    override val name = "ChatApiInitialiser"

    override operator fun invoke() {
        chatApiListenerCoordinator.register()
    }
}

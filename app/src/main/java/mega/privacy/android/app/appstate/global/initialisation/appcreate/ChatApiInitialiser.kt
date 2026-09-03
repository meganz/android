package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.app.globalmanagement.ChatApiListenerCoordinator
import mega.privacy.android.navigation.contract.initialisation.CriticalAsyncAppCreateInitialiser
import javax.inject.Inject

/**
 * Registers the global chat SDK listeners via [ChatApiListenerCoordinator].
 *
 * Critical: chat request, notification and call listeners must be attached before any chat
 * activity can occur, exactly as `setupMegaChatApi` ran synchronously in `onCreate`.
 *
 * The coordinator is lazy so that constructing this initialiser never constructs the chat SDK
 * instance it holds, whose creation blocks until the native library has finished loading.
 */
internal class ChatApiInitialiser @Inject constructor(
    private val chatApiListenerCoordinator: dagger.Lazy<ChatApiListenerCoordinator>,
) : CriticalAsyncAppCreateInitialiser {
    override val name = "ChatApiInitialiser"

    override suspend operator fun invoke() {
        chatApiListenerCoordinator.get().register()
    }
}

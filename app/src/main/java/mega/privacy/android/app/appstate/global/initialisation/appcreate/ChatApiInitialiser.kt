package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.app.globalmanagement.ChatApiListenerCoordinator
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppCreateInitialiser
import javax.inject.Inject

/**
 * Registers the global chat SDK listeners via [ChatApiListenerCoordinator].
 *
 * Critical: chat request, notification and call listeners must be attached before any chat
 * activity can occur, exactly as `setupMegaChatApi` ran synchronously in `onCreate`.
 */
internal class ChatApiInitialiser @Inject constructor(
    private val chatApiListenerCoordinator: ChatApiListenerCoordinator,
) : AppCreateInitialiser {
    override val name = "ChatApiInitialiser"
    override val isCritical = true

    override suspend operator fun invoke() {
        chatApiListenerCoordinator.register()
    }
}

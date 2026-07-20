package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.app.receivers.GlobalNetworkStateHandler
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import javax.inject.Inject

/**
 * Starts [GlobalNetworkStateHandler]'s connectivity collection.
 *
 * Non-critical: the handler only reacts to future connectivity changes with reconnect/retry
 * calls, so starting it fire-and-forget alongside the other async boot work is safe.
 */
internal class NetworkStateInitialiser @Inject constructor(
    private val globalNetworkStateHandler: GlobalNetworkStateHandler,
) : AsyncAppCreateInitialiser {
    override val name = "NetworkStateInitialiser"

    override suspend operator fun invoke() {
        globalNetworkStateHandler.start()
    }
}

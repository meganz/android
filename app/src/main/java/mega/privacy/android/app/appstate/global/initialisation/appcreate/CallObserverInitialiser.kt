package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.app.globalmanagement.CallChangesObserver
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser
import javax.inject.Inject

/**
 * Starts [CallChangesObserver]'s call and session update collections.
 *
 * Critical: the body only launches the collections (non-blocking), and running it synchronously
 * keeps call handling armed before any Activity or push can surface a call.
 */
internal class CallObserverInitialiser @Inject constructor(
    private val callChangesObserver: CallChangesObserver,
) : SynchronousAppCreateInitialiser {
    override val name = "CallObserverInitialiser"

    override operator fun invoke() {
        callChangesObserver.init()
    }
}

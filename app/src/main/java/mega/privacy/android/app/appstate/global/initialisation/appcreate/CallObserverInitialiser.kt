package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.app.globalmanagement.CallChangesObserver
import mega.privacy.android.navigation.contract.initialisation.CriticalAsyncAppCreateInitialiser
import javax.inject.Inject

/**
 * Starts [CallChangesObserver]'s call and session update collections.
 *
 * Critical: the body only launches the collections (non-blocking), and running it in the
 * critical phase keeps call handling armed before any Activity or push can surface a call.
 *
 * The observer is lazy so that constructing this initialiser never constructs the chat SDK
 * instance it holds, whose creation blocks until the native library has finished loading.
 */
internal class CallObserverInitialiser @Inject constructor(
    private val callChangesObserver: dagger.Lazy<CallChangesObserver>,
) : CriticalAsyncAppCreateInitialiser {
    override val name = "CallObserverInitialiser"

    override suspend operator fun invoke() {
        callChangesObserver.get().init()
    }
}

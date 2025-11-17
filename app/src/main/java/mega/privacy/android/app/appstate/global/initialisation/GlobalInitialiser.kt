package mega.privacy.android.app.appstate.initialisation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mega.privacy.android.app.appstate.initialisation.initialisers.AppStartInitialiser
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.app.appstate.initialisation.initialisers.PreLoginInitialiser
import mega.privacy.android.domain.qualifier.ApplicationScope
import timber.log.Timber
import javax.inject.Inject

/**
 * Auth initialiser handles initialisation tasks during user auth.
 * It's an abstraction that simplifies the logic in the [mega.privacy.android.app.appstate.global.GlobalStateViewModel].
 *
 * @property coroutineScope
 * @property appStartInitialisers
 * @property preLoginInitialisers
 * @property postLoginInitialisers
 */
class GlobalInitialiser @Inject constructor(
    @ApplicationScope private val coroutineScope: CoroutineScope,
    private val appStartInitialisers: Set<@JvmSuppressWildcards AppStartInitialiser>,
    private val preLoginInitialisers: Set<@JvmSuppressWildcards PreLoginInitialiser>,
    private val postLoginInitialisers: Set<@JvmSuppressWildcards PostLoginInitialiser>,
) {
    private var onPreLoginJob: Job? = null
    private var onPostLoginJob: Job? = null

    fun onAppStart() {
        appStartInitialisers.forEach {
            coroutineScope.launch {
                try {
                    it()
                } catch (e: Exception) {
                    Timber.e(e, "Error during auth viewmodel initialisation")
                }
            }
        }
    }

    fun onPreLogin(session: String?) {
        onPreLoginJob?.cancel()
        onPreLoginJob = coroutineScope.launch {
            preLoginInitialisers.forEach { initialiser ->
                launch {
                    try {
                        initialiser(session)
                    } catch (e: Exception) {
                        Timber.e(e, "Error during pre-login initialisation")
                    }
                }
            }
        }
    }

    fun onPostLogin(session: String, isFastLogin: Boolean) {
        onPostLoginJob?.cancel()
        onPostLoginJob = coroutineScope.launch {
            postLoginInitialisers.forEach { initialiser ->
                launch {
                    try {
                        initialiser(session, isFastLogin)
                    } catch (e: Exception) {
                        Timber.e(e, "Error during post-login initialisation")
                    }
                }
            }
        }
    }
}
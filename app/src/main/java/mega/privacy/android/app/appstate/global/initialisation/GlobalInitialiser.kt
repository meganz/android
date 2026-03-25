package mega.privacy.android.app.appstate.global.initialisation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiser
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiser
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth initialiser handles initialisation tasks during user auth.
 * It's an abstraction that simplifies the logic in the [mega.privacy.android.app.appstate.global.GlobalStateViewModel].
 *
 * @property coroutineScope
 * @property appStartInitialisers
 * @property preLoginInitialisers
 * @property postLoginInitialisers
 */
@Singleton
class GlobalInitialiser @Inject constructor(
    @ApplicationScope private val coroutineScope: CoroutineScope,
    private val appStartInitialisers: Set<@JvmSuppressWildcards AppStartInitialiser>,
    private val postLoginInitialisers: Set<@JvmSuppressWildcards PostLoginInitialiser>,
) {
    private var onAppStartJob: Job? = null
    private var onPostLoginJob: Job? = null

    fun onAppStart() {
        if (onAppStartJob?.isActive != true) {
            onAppStartJob = coroutineScope.launch {
                appStartInitialisers.forEach {
                    launch {
                        try {
                            it()
                        } catch (e: Exception) {
                            Timber.e(e, "Error during auth viewmodel initialisation")
                        }
                    }
                }
            }
        }
    }

    fun onPostLogin(session: String, isFastLogin: Boolean) {
        Timber.d("Starting post-login initialisation")
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
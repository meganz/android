package mega.privacy.android.app.appstate.global.initialisation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppCreateInitialiser
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiser
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiser
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth initialiser handles initialisation tasks during user auth.
 *
 * @property coroutineScope
 * @property appCreateInitialisers ordered list of app-create initialisers
 * @property appStartInitialisers
 * @property postLoginInitialisers
 */
@Singleton
class GlobalInitialiser @Inject constructor(
    @ApplicationScope private val coroutineScope: CoroutineScope,
    private val appCreateInitialisers: List<@JvmSuppressWildcards AppCreateInitialiser>,
    private val appStartInitialisers: Set<@JvmSuppressWildcards AppStartInitialiser>,
    private val postLoginInitialisers: dagger.Lazy<Set<@JvmSuppressWildcards PostLoginInitialiser>>,
) {
    private var onAppCreateCalled = false
    private var onAppStartJob: Job? = null
    private var onPostLoginJob: Job? = null

    /**
     * Runs the app-create initialisers once per process, from `Application.onCreate`.
     *
     * Critical initialisers run synchronously in list order and complete before this method
     * returns; their failures propagate to the caller. Non-critical initialisers are launched
     * fire-and-forget into the application scope with failures logged.
     *
     * @param filter restricts which initialisers run, allowing tests to boot selectively
     */
    fun onAppCreate(filter: (AppCreateInitialiser) -> Boolean = { true }) {
        if (onAppCreateCalled) return
        onAppCreateCalled = true
        val (critical, nonCritical) = appCreateInitialisers
            .filter(filter)
            .partition { it.isCritical }
        if (critical.isNotEmpty()) {
            runBlocking {
                critical.forEach { it() }
            }
        }
        nonCritical.forEach { initialiser ->
            coroutineScope.launch {
                try {
                    initialiser()
                } catch (e: Exception) {
                    Timber.e(e, "Error during app create initialisation: ${initialiser.name}")
                }
            }
        }
    }

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
            postLoginInitialisers.get().forEach { initialiser ->
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

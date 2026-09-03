package mega.privacy.android.app.appstate.global.initialisation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.navigation.contract.initialisation.AppCreateInitialiser
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import mega.privacy.android.navigation.contract.initialisation.CriticalAsyncAppCreateInitialiser
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiser
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiser
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth initialiser handles initialisation tasks during user auth.
 *
 * @property coroutineScope
 * @property syncAppCreateInitialisers ordered list of app-create initialisers
 * @property criticalAsyncAppCreateInitialisers ordered list of critical app-create initialisers
 * @property asyncAppCreateInitialisers set of app-create initialisers
 * @property appStartInitialisers
 * @property postLoginInitialisers
 */
@Singleton
class GlobalInitialiser @Inject constructor(
    @ApplicationScope private val coroutineScope: CoroutineScope,
    private val syncAppCreateInitialisers: dagger.Lazy<List<@JvmSuppressWildcards SynchronousAppCreateInitialiser>>,
    private val criticalAsyncAppCreateInitialisers: dagger.Lazy<List<@JvmSuppressWildcards CriticalAsyncAppCreateInitialiser>>,
    private val asyncAppCreateInitialisers: dagger.Lazy<Set<@JvmSuppressWildcards AsyncAppCreateInitialiser>>,
    private val appStartInitialisers: dagger.Lazy<Set<@JvmSuppressWildcards AppStartInitialiser>>,
    private val postLoginInitialisers: dagger.Lazy<Set<@JvmSuppressWildcards PostLoginInitialiser>>,
) {
    private var onAppCreateCalled = false
    private var criticalAsyncJob: Job = Job().apply { complete() }
    private var onAppStartJob: Job? = null
    private var onPostLoginJob: Job? = null

    /**
     * Runs the app-create initialisers once per process, from `Application.onCreate`.
     *
     * Synchronous initialisers run in list order and complete before this method returns; their
     * failures propagate to the caller. [CriticalAsyncAppCreateInitialiser]s then run to
     * completion sequentially, in list order, on the application scope, before the remaining
     * async initialisers are launched fire-and-forget with failures logged. The app-start and
     * post-login phases also wait for the critical async initialisers.
     *
     * @param filter restricts which initialisers run, allowing tests to boot selectively
     */
    fun onAppCreate(filter: (AppCreateInitialiser) -> Boolean = { true }) {
        if (onAppCreateCalled) return
        onAppCreateCalled = true
        syncAppCreateInitialisers.get().filter(filter).forEach {
            it()
        }
        criticalAsyncJob = coroutineScope.launch {
            criticalAsyncAppCreateInitialisers.get().filter(filter)
                .forEach { initialiser ->
                    try {
                        initialiser()
                    } catch (e: Exception) {
                        Timber.e(e, "Error during app create initialisation: ${initialiser.name}")
                    }
                }
        }
        coroutineScope.launch {
            criticalAsyncJob.join()
            asyncAppCreateInitialisers.get().filter(filter)
                .forEach { initialiser ->
                    launch {
                        try {
                            initialiser()
                        } catch (e: Exception) {
                            Timber.e(e, "Error during app create initialisation: ${initialiser.name}")
                        }
                    }
                }
        }
    }

    fun onAppStart() {
        if (onAppStartJob?.isActive != true) {
            onAppStartJob = coroutineScope.launch {
                criticalAsyncJob.join()
                appStartInitialisers.get().forEach {
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
            criticalAsyncJob.join()
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

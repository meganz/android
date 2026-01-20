package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.usecase.contact.ReloadContactDatabase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Reload contact database initialiser
 * Reloads the contact database after login to ensure all contact information is up to date
 * Waits for fetch nodes to complete before executing to ensure user data is fully loaded
 *
 * @param reloadContactDatabase Use case to reload the contact database
 * @param monitorFetchNodesFinishUseCase Use case to monitor when fetch nodes is complete
 */
class ReloadContactDatabaseInitialiser @Inject constructor(
    private val reloadContactDatabase: ReloadContactDatabase,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
) : PostLoginInitialiser(
    action = { _, isFastLogin ->
        runCatching {
            monitorFetchNodesFinishUseCase().collectLatest { isFinish ->
                if (isFinish) {
                    reloadContactDatabase(!isFastLogin)
                }
            }
        }.onFailure { e ->
            Timber.e(e, "Error re-loading contacts DB")
        }
    }
)


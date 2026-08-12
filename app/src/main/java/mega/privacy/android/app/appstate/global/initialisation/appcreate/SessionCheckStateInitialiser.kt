package mega.privacy.android.app.appstate.global.initialisation.appcreate

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.MonitorLogoutUseCase
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Populates and maintains [SessionCheckState]: root node existence is computed on app create and
 * recomputed whenever fetch nodes finishes or a logout occurs; credentials mirror the credentials
 * preference flow.
 *
 * Async: nothing at boot waits on it; readers handle the brief null window per the
 * [SessionCheckState] contract.
 */
@Singleton
internal class SessionCheckStateInitialiser @Inject constructor(
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val monitorLogoutUseCase: MonitorLogoutUseCase,
    private val monitorUserCredentialsUseCase: MonitorUserCredentialsUseCase,
) : AsyncAppCreateInitialiser, SessionCheckState {
    override val name = "SessionCheckStateInitialiser"

    private val _rootNodeExists = MutableStateFlow<Boolean?>(null)
    override val rootNodeExists = _rootNodeExists.asStateFlow()

    private val _userCredentials = MutableStateFlow<UserCredentials?>(null)
    override val userCredentials = _userCredentials.asStateFlow()

    override suspend operator fun invoke(): Unit = coroutineScope {
        launch {
            monitorUserCredentialsUseCase()
                .retry {
                    Timber.e(it, "$name: Error monitoring user credentials")
                    true
                }
                .collect { _userCredentials.value = it }
        }

        launch {
            _rootNodeExists.value = currentRootNodeExists()
            merge(
                monitorFetchNodesFinishUseCase(),
                monitorLogoutUseCase(),
            )
                .retry {
                    Timber.e(it, "$name: Error monitoring session events")
                    true
                }
                .collect { _rootNodeExists.value = currentRootNodeExists() }
        }
    }

    private suspend fun currentRootNodeExists(): Boolean? = runCatching {
        rootNodeExistsUseCase()
    }.onFailure {
        Timber.e(it, "$name: Error checking root node existence")
    }.getOrNull()
}

package mega.privacy.android.app.appstate.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.privacy.android.app.appstate.global.mapper.BlockedStateMapper
import mega.privacy.android.app.appstate.global.model.BlockedState
import mega.privacy.android.app.appstate.global.model.GlobalState
import mega.privacy.android.app.appstate.global.model.RefreshEvent
import mega.privacy.android.app.appstate.global.model.RootNodeState
import mega.privacy.android.app.appstate.initialisation.GlobalInitialiser
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.extension.onFirst
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.HandleBlockedStateSessionUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GlobalStateViewModel @Inject constructor(
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val monitorUserCredentialsUseCase: MonitorUserCredentialsUseCase,
    private val globalInitialiser: GlobalInitialiser,
    private val monitorAccountBlockedUseCase: MonitorAccountBlockedUseCase,
    private val blockedStateMapper: BlockedStateMapper,
    private val handleBlockedStateSessionUseCase: HandleBlockedStateSessionUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
) : ViewModel() {
    private val refreshSessionFlow = MutableSharedFlow<RefreshEvent>()

    init {
        globalInitialiser.onAppStart()
    }

    val state: StateFlow<GlobalState> by lazy {
        getStateValues().catch {
            Timber.e(it, "Error while building auth state")
        }.distinctUntilChanged()
            .onEach {
                Timber.d("AuthState emitted: $it")
            }.asUiStateFlow(
                scope = viewModelScope,
                initialValue = GlobalState.Loading(ThemeMode.System)
            )
    }

    private fun getStateValues(): Flow<GlobalState> {
        return monitorBlockedState().map { blockedState ->
            if (blockedState is BlockedState.NotBlocked && blockedState.session != null) {
                { themeMode: ThemeMode -> GlobalState.LoggedIn(themeMode, blockedState.session) }
            } else {
                { themeMode: ThemeMode -> GlobalState.RequireLogin(themeMode, blockedState) }
            }
        }.flatMapLatest { stateFunction ->
            monitorThemeModeUseCase().catch {
                Timber.e(it, "Error monitoring theme mode")
                emit(ThemeMode.System)
            }.map { themeMode ->
                stateFunction(themeMode)
            }
        }
    }

    val rootNodeExistsFlow: StateFlow<RootNodeState> by lazy {
        merge(
            monitorFetchNodesFinishUseCase()
                .onStart { emit(doesRootNodeExist()) }
                .map { RootNodeState(it) }
                .catch { Timber.e(it, "Error monitoring fetch nodes finish") },
            getPostLoginSessionFlow()
                .catch { Timber.e(it, "Error monitoring post login session") }
                .emitFalseOnLogout(),
            refreshSessionFlow.map {
                RootNodeState(false, it)
            }
        ).catch { Timber.e(it, "Error monitoring fetch nodes finish") }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = RootNodeState(false)
            )
    }

    private suspend fun doesRootNodeExist(): Boolean =
        runCatching { rootNodeExistsUseCase() }.onFailure { Timber.e(it) }.getOrDefault(false)

    private fun getPostLoginSessionFlow(): Flow<String?> = sessionFlow.dropWhile { it != null }

    private fun Flow<String?>.emitFalseOnLogout() = this.filter { it == null }
        .map { RootNodeState(false) }


    private fun monitorBlockedState() =
        combine(
            sessionFlow,
            monitorAccountBlockedUseCase(),
        ) { session, blockedState ->
            val result = blockedStateMapper(blockedState, session)
            handleBlockedStateSessionUseCase(blockedState)
            result
        }

    /**
     * Queue a message to be displayed in the snackbar
     */
    fun queueMessage(message: String) {
        viewModelScope.launch {
            snackbarEventQueue.queueMessage(message)
        }
    }

    fun refreshSession(event: RefreshEvent) {
        viewModelScope.launch {
            refreshSessionFlow.emit(event)
        }
    }

    private val sessionFlow: SharedFlow<String?> by lazy {
        monitorUserCredentialsUseCase()
            .catch { Timber.e(it, "Error monitoring user credentials") }
            .map { it?.session }
            .distinctUntilChanged()
            .onFirst(
                predicate = { true },
                action = { globalInitialiser.onPreLogin(it) }
            ).shareIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                replay = 1,
            )
    }
}
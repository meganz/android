package mega.privacy.android.app.appstate.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.app.appstate.global.mapper.BlockedStateMapper
import mega.privacy.android.app.appstate.global.model.BlockedState
import mega.privacy.android.app.appstate.global.model.GlobalState
import mega.privacy.android.app.appstate.initialisation.GlobalInitialiser
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.extension.onFirst
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.HandleBlockedStateSessionUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.shared.original.core.ui.utils.asUiStateFlow
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
) : ViewModel() {
    init {
        globalInitialiser.onAppStart()
    }

    private var lastLoggedInSession: String? = null

    val state: StateFlow<GlobalState> by lazy {
        getStateValues().onEach { state ->
            if (state is GlobalState.LoggedIn) {
                if (lastLoggedInSession != state.session) {
                    lastLoggedInSession = state.session
                    globalInitialiser.onPostLogin(state.session)
                }
            }
        }.catch {
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


    private fun monitorBlockedState() =
        combine(
            sessionFlow,
            monitorAccountBlockedUseCase(),
        ) { session, blockedState ->
            val result = blockedStateMapper(blockedState, session)
            handleBlockedStateSessionUseCase(blockedState)
            result
        }


    private val sessionFlow: Flow<String?> by lazy {
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
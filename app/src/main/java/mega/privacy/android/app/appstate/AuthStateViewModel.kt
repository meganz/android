package mega.privacy.android.app.appstate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.appstate.initialisation.AuthInitialiser
import mega.privacy.android.app.appstate.mapper.BlockedStateMapper
import mega.privacy.android.app.appstate.model.AuthState
import mega.privacy.android.app.appstate.model.BlockedState
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.extension.onFirst
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.shared.original.core.ui.utils.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AuthStateViewModel @Inject constructor(
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val monitorUserCredentialsUseCase: MonitorUserCredentialsUseCase,
    private val authInitialiser: AuthInitialiser,
    private val monitorAccountBlockedUseCase: MonitorAccountBlockedUseCase,
    private val blockedStateMapper: BlockedStateMapper,
) : ViewModel() {

    private val handleBlockedStateSessionUseCase: suspend (AccountBlockedEvent) -> Unit = {
        //Create a use case that logs out the and cleans up resources if need be based on blocked state
        //then inject it as a parameter
    }

    init {
        authInitialiser.onAppStart()
    }

    val state: StateFlow<AuthState> by lazy {
        getStateValues().onEach { state ->
            if (state is AuthState.LoggedIn) {
                authInitialiser.onPostLogin(state.session)
            }
        }.catch {
            Timber.e(it, "Error while building auth state")
        }.onEach {
            Timber.d("AuthState emitted: $it")
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = AuthState.Loading(ThemeMode.System)
        )
    }

    private fun getStateValues(): Flow<AuthState> {
        return monitorBlockedState().map { blockedState ->
            if (blockedState is BlockedState.NotBlocked && blockedState.session != null) {
                { themeMode: ThemeMode -> AuthState.LoggedIn(themeMode, blockedState.session) }
            } else {
                { themeMode: ThemeMode -> AuthState.RequireLogin(themeMode, blockedState) }
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
            monitorCredentials(),
            monitorAccountBlockedUseCase(),
        ) { credentials, blockedState ->
            val session = credentials?.session
            val result = blockedStateMapper(blockedState, session)
            handleBlockedStateSessionUseCase(blockedState)
            result
        }


    private fun monitorCredentials(): Flow<UserCredentials?> = monitorUserCredentialsUseCase()
        .onFirst(
            predicate = { true },
            action = { authInitialiser.onPreLogin(it?.session) }
        )
}
package mega.privacy.android.app.appstate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.appstate.initialisation.AuthInitialiser
import mega.privacy.android.app.appstate.model.AuthState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.extension.onFirst
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
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
) : ViewModel() {

    init {
        authInitialiser.onAppStart()
    }

    val state: StateFlow<AuthState> by lazy {
        getStateValues().map { (themeMode, credentials) ->
            when (val session = credentials?.session) {
                null -> AuthState.RequireLogin(themeMode)
                else -> {
                    authInitialiser.onPostLogin(session)
                    AuthState.LoggedIn(themeMode, credentials)
                }
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

    private fun getStateValues(): Flow<Pair<ThemeMode, UserCredentials?>> {
        return monitorUserCredentialsUseCase().onFirst(
            predicate = { true },
            action = { authInitialiser.onPreLogin(it?.session) }).flatMapLatest { credentials ->
            monitorThemeModeUseCase().catch {
                Timber.e(it, "Error monitoring theme mode")
                emit(ThemeMode.System)
            }.map { themeMode ->
                Pair(themeMode, credentials)
            }
        }
    }
}
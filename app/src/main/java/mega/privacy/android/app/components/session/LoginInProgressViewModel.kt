package mega.privacy.android.app.components.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import javax.inject.Inject

/**
 * View model for login in progress state.
 * Exposes whether login is currently in progress (mutex locked).
 */
@HiltViewModel
class LoginInProgressViewModel @Inject constructor(
    @LoginMutex private val loginMutex: Mutex,
) : ViewModel() {

    val state = flow {
        if (loginMutex.isLocked) {
            loginMutex.withLock {
                emit(LoginInProgressState(isLoginInProgress = false))
            }
        } else {
            emit(LoginInProgressState(isLoginInProgress = false))
        }
    }.asUiStateFlow(
        scope = viewModelScope,
        initialValue = LoginInProgressState(isLoginInProgress = loginMutex.isLocked),
    )

}

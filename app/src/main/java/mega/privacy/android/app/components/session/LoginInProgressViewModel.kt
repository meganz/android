package mega.privacy.android.app.components.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.chat.IsMegaApiLoggedInUseCase
import javax.inject.Inject

/**
 * View model for login in progress state.
 * Exposes whether login is currently in progress (mutex locked).
 */
@HiltViewModel
class LoginInProgressViewModel @Inject constructor(
    @LoginMutex private val loginMutex: Mutex,
    private val isMegaApiLoggedInUseCase: IsMegaApiLoggedInUseCase,
) : ViewModel() {

    val state = flow {
        while (!loginMutex.isLocked) {
            delay(POLL_INTERVAL_MS)
            // there are some case mutex locked by fetch node but login already completed
            if (isMegaApiLoggedInUseCase()) break
        }
        emit(LoginInProgressState(isLoginInProgress = false))
    }.catch {
        emit(LoginInProgressState(isLoginInProgress = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LoginInProgressState(isLoginInProgress = loginMutex.isLocked),
    )

    companion object {
        private const val POLL_INTERVAL_MS = 100L
    }
}

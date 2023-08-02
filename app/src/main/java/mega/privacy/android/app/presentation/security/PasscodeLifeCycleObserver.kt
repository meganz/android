package mega.privacy.android.app.presentation.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.passcode.SetAppPausedTimeUseCase
import mega.privacy.android.domain.usecase.passcode.UpdatePasscodeStateUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Passcode life cycle observer
 *
 * @property setAppPausedTimeUseCase
 * @property updatePasscodeStateUseCase
 */
@Singleton
class PasscodeLifeCycleObserver @Inject constructor(
    private val setAppPausedTimeUseCase: SetAppPausedTimeUseCase,
    private val updatePasscodeStateUseCase: UpdatePasscodeStateUseCase,
) : DefaultLifecycleObserver {

    override fun onPause(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            Timber.d("App paused")
            setAppPausedTimeUseCase(System.currentTimeMillis())
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            Timber.d("App resumed")
            updatePasscodeStateUseCase(System.currentTimeMillis())
        }
    }
}
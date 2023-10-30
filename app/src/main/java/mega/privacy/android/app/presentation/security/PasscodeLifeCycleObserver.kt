package mega.privacy.android.app.presentation.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
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
    @ApplicationScope private val scope: CoroutineScope,
) : PasscodeProcessLifeCycleObserver {

    override fun onStart(data: PasscodeProcessLifeCycleEventData) {
        scope.launch {
            Timber.d("App started")
            updatePasscodeStateUseCase(System.currentTimeMillis(), data.orientation)
        }
    }

    override fun onStop(data: PasscodeProcessLifeCycleEventData) {
        scope.launch {
            Timber.d("App paused")
            setAppPausedTimeUseCase(System.currentTimeMillis(), data.orientation)
        }
    }
}
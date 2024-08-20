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
    private val setAppPausedTimeUseCase: dagger.Lazy<SetAppPausedTimeUseCase>,
    private val updatePasscodeStateUseCase: dagger.Lazy<UpdatePasscodeStateUseCase>,
    @ApplicationScope private val scope: dagger.Lazy<CoroutineScope>,
) : PasscodeProcessLifeCycleObserver {

    override fun onStart(data: PasscodeProcessLifeCycleEventData) {
        scope.get().launch {
            Timber.d("App started")
            updatePasscodeStateUseCase.get()(System.currentTimeMillis(), data.orientation)
        }
    }

    override fun onStop(data: PasscodeProcessLifeCycleEventData) {
        scope.get().launch {
            Timber.d("App paused")
            setAppPausedTimeUseCase.get()(System.currentTimeMillis(), data.orientation)
        }
    }
}
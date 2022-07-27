package mega.privacy.android.app.presentation.featureflag

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.ApplicationScope
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.usecase.ShakeDetector
import mega.privacy.android.app.domain.usecase.VibrateDevice
import javax.inject.Inject

/**
 * View model for shake detection
 */
class ShakeDetectorViewModel @Inject constructor(
    @ApplicationScope private val coroutineScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val vibrateDevice: VibrateDevice,
    private val shakeDetector: ShakeDetector,
) {

    private val _state = MutableStateFlow(false)

    /**
     * State to be absorbed by UI
     */
    val state: MutableStateFlow<Boolean> = _state

    /**
     * function to register shake listener & catch shake event
     */
    fun registerAndCatchShakeEvent() {
        coroutineScope.launch(ioDispatcher) {
            shakeDetector().collect {
                state.update { true }
                vibrateDevice()
            }
        }
    }
}
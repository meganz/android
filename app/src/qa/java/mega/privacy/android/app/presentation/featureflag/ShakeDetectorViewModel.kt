package mega.privacy.android.app.presentation.featureflag

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.DetectShake
import mega.privacy.android.app.domain.usecase.VibrateDevice
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * View model for shake detection
 */
class ShakeDetectorViewModel @Inject constructor(
    @ApplicationScope private val coroutineScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val vibrateDevice: VibrateDevice,
    private val detectShake: DetectShake,
) {

    private val _state = MutableStateFlow(false)

    /**
     * State to be absorbed by UI
     */
    val state = _state.asStateFlow()

    /**
     * function to register shake listener & catch shake event
     */
    fun registerAndCatchShakeEvent() {
        coroutineScope.launch(ioDispatcher) {
            detectShake().collect {
                _state.value = true
                vibrateDevice()
            }
        }
    }
}
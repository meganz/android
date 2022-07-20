package mega.privacy.android.app.presentation.featureflag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.ApplicationScope
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.usecase.ShakeDetectorUseCase
import mega.privacy.android.app.domain.usecase.VibrateDeviceUseCase
import javax.inject.Inject

/**
 * View model for shake detection
 */
class ShakeDetectorViewModel @Inject constructor(
    @ApplicationScope private val coroutineScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val vibrateDeviceUseCase: VibrateDeviceUseCase,
    private val shakeDetectorUseCase: ShakeDetectorUseCase,
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
            shakeDetectorUseCase().collect {
                state.update { true }
                vibrateDeviceUseCase()
            }
        }
    }
}
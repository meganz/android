package mega.privacy.android.app.presentation.featureflag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.usecase.ShakeDetectorUseCase
import mega.privacy.android.app.domain.usecase.VibrateDeviceUseCase
import javax.inject.Inject

/**
 * View model for shake detection
 */
@HiltViewModel
class ShakeDetectorViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val vibrateDeviceUseCase: VibrateDeviceUseCase,
    private val shakeDetectorUseCase: ShakeDetectorUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(false)

    /**
     * State to be absorbed by UI
     */
    val state: MutableStateFlow<Boolean> = _state

    /**
     * function to register shake listener & catch shake event
     */
    fun registerAndCatchShakeEvent() {
        viewModelScope.launch(ioDispatcher) {
            shakeDetectorUseCase().collect {
                state.update { true }
                vibrateDeviceUseCase()
            }
        }
    }
}
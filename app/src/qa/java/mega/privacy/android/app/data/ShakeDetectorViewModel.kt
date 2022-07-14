package mega.privacy.android.app.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.usecase.GetVibrateCountUseCase
import mega.privacy.android.app.domain.usecase.ShakeDetectorUseCase
import mega.privacy.android.app.domain.usecase.VibrateDeviceUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShakeDetectorViewModel @Inject constructor(
    val vibrateDeviceUseCase: VibrateDeviceUseCase,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
    val shakeDetectorUseCase: ShakeDetectorUseCase,
    val getVibrateCountUseCase: GetVibrateCountUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(Boolean)
    val state: MutableStateFlow<Boolean.Companion> = _state

    /*@Inject
    lateinit var shakeDetectorUseCase: ShakeDetectorUseCase*/

    /*init {
        getVibrateCountUseCase().map {
            state.update { it }
        }
    }*/

    fun invokeShake() {
        shakeDetectorUseCase.invoke()
    }

    fun catchShakeEvent() {
        viewModelScope.launch {
            shakeDetectorUseCase().collect {
                vibrateDeviceUseCase()
            }
        }
    }
}
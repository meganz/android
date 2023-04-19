package mega.privacy.android.app.presentation.slideshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.slideshow.model.SlideshowSettingViewState
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.usecase.MonitorSlideshowOrderSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowRepeatSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowSpeedSettingUseCase
import mega.privacy.android.domain.usecase.SaveSlideshowOrderSettingUseCase
import mega.privacy.android.domain.usecase.SaveSlideshowRepeatSettingUseCase
import mega.privacy.android.domain.usecase.SaveSlideshowSpeedSettingUseCase
import javax.inject.Inject

/**
 * ViewModel for slideshow
 *
 */
@HiltViewModel
class SlideshowSettingViewModel @Inject constructor(
    private val monitorSlideshowOrderSettingUseCase: MonitorSlideshowOrderSettingUseCase,
    private val monitorSlideshowSpeedSettingUseCase: MonitorSlideshowSpeedSettingUseCase,
    private val monitorSlideshowRepeatSettingUseCase: MonitorSlideshowRepeatSettingUseCase,
    private val saveSlideshowOrderSettingUseCase: SaveSlideshowOrderSettingUseCase,
    private val saveSlideshowSpeedSettingUseCase: SaveSlideshowSpeedSettingUseCase,
    private val saveSlideshowRepeatSettingUseCase: SaveSlideshowRepeatSettingUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SlideshowSettingViewState())
    val state = _state.asStateFlow()

    init {
        monitorOrderSetting()
        monitorSpeedSetting()
        monitorRepeatSetting()
    }

    private fun monitorOrderSetting() = monitorSlideshowOrderSettingUseCase()
        .distinctUntilChanged().onEach { order ->
            _state.update {
                it.copy(
                    order = order ?: SlideshowOrder.Shuffle,
                )
            }
        }.launchIn(viewModelScope)

    private fun monitorSpeedSetting() = monitorSlideshowSpeedSettingUseCase()
        .distinctUntilChanged().onEach { speed ->
            _state.update {
                it.copy(speed = speed ?: SlideshowSpeed.Normal)
            }
        }.launchIn(viewModelScope)

    private fun monitorRepeatSetting() = monitorSlideshowRepeatSettingUseCase()
        .distinctUntilChanged().onEach { isRepeat ->
            _state.update {
                it.copy(repeat = isRepeat ?: false)
            }
        }.launchIn(viewModelScope)

    fun saveOrderSetting(order: SlideshowOrder) = viewModelScope.launch {
        saveSlideshowOrderSettingUseCase(order)
    }

    fun saveSpeedSetting(speed: SlideshowSpeed) = viewModelScope.launch {
        saveSlideshowSpeedSettingUseCase(speed)
    }

    fun saveRepeatSetting(isRepeat: Boolean) = viewModelScope.launch {
        saveSlideshowRepeatSettingUseCase(isRepeat)
    }
}

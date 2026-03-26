package mega.privacy.mobile.home.presentation.home.widget.chips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.mobile.home.presentation.home.widget.chips.model.HomeChipsUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [HomeChipsWidget].
 *
 * Reads [ApiFeatures.MediaRevampPhase2] and [ApiFeatures.AudiosChipInHome].
 * The Audios chip navigates to `AudioSectionNavKey`; the app gates Compose vs legacy audio by flag.
 */
@HiltViewModel
class HomeChipsWidgetViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeChipsUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorMediaRevampPhase2Flag().catch { Timber.e(it) },
            monitorAudiosChipInHomeFlag().catch { Timber.e(it) },
        ) { isMediaRevampPhase2Enabled, isAudiosChipVisible ->
            HomeChipsUiState(
                isMediaRevampPhase2Enabled = isMediaRevampPhase2Enabled,
                isAudiosChipVisible = isAudiosChipVisible,
            )
        }.asUiStateFlow(viewModelScope, HomeChipsUiState())
    }

    private fun monitorMediaRevampPhase2Flag() = flow {
        emit(getFeatureFlagValueUseCase(ApiFeatures.MediaRevampPhase2))
    }

    private fun monitorAudiosChipInHomeFlag() = flow {
        emit(getFeatureFlagValueUseCase(ApiFeatures.AudiosChipInHome))
    }
}

package mega.privacy.mobile.home.presentation.home.widget.chips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
 * Reads the [ApiFeatures.MediaRevampPhase2] flag and produces an ordered list of [HomeChip]s,
 * hiding the Videos chip when the flag is enabled.
 */
@HiltViewModel
class HomeChipsWidgetViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeChipsUiState> by lazy {
        monitorMediaRevampPhase2Flag()
            .catch { Timber.e(it) }
            .map { isMediaRevampPhase2Enabled ->
                HomeChipsUiState(isMediaRevampPhase2Enabled = isMediaRevampPhase2Enabled)
            }
            .asUiStateFlow(viewModelScope, HomeChipsUiState())
    }

    private fun monitorMediaRevampPhase2Flag() = flow {
        emit(getFeatureFlagValueUseCase(ApiFeatures.MediaRevampPhase2))
    }
}

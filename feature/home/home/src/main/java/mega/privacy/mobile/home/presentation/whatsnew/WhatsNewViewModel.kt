package mega.privacy.mobile.home.presentation.whatsnew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.usecase.global.GetAppVersionUseCase
import mega.privacy.android.domain.usecase.home.MarkNewFeatureDisplayedUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import javax.inject.Inject

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val whatsNewDetails: Map<String, @JvmSuppressWildcards WhatsNewDetail>,
    private val getAppVersionUseCase: GetAppVersionUseCase,
    private val markNewFeatureDisplayedUseCase: MarkNewFeatureDisplayedUseCase
) : ViewModel() {

    val uiState: StateFlow<WhatsNewUiState> by lazy {
        flow {
            val appVersion = getAppVersionUseCase()
            val versionString = "${appVersion?.major}.${appVersion?.minor}"
            whatsNewDetails[versionString]?.let { detail ->
                // Mark the new feature as displayed
                markNewFeatureDisplayedUseCase()
                emit(WhatsNewUiState.Ready(detail))
            }
        }.asUiStateFlow(
            viewModelScope,
            WhatsNewUiState.Loading
        )
    }
}
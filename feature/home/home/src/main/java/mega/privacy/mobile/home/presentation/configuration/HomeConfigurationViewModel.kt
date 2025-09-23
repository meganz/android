package mega.privacy.mobile.home.presentation.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.mobile.home.presentation.configuration.model.HomeConfigurationUiState
import javax.inject.Inject

@HiltViewModel
class HomeConfigurationViewModel @Inject constructor() : ViewModel() {
    val state: StateFlow<HomeConfigurationUiState> by lazy {
        flow {
            emit(HomeConfigurationUiState.Loading)
        }
            .asUiStateFlow(
                viewModelScope,
                HomeConfigurationUiState.Loading,
            )
    }
}

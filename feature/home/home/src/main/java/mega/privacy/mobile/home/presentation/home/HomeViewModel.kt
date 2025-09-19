package mega.privacy.mobile.home.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.shared.original.core.ui.utils.asUiStateFlow
import mega.privacy.mobile.home.presentation.home.model.HomeUiState
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    val state: StateFlow<HomeUiState> by lazy {
        flow {
            emit(HomeUiState.Loading)
        }
            .asUiStateFlow(
                viewModelScope,
                HomeUiState.Loading,
            )
    }
}

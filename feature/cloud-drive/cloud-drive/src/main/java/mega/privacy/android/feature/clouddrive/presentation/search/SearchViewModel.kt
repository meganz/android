package mega.privacy.android.feature.clouddrive.presentation.search

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.navigation.destination.SearchNavKey

@HiltViewModel(assistedFactory = SearchViewModel.Factory::class)
class SearchViewModel @AssistedInject constructor(
    @Assisted private val navKey: SearchNavKey,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    internal val uiState = _uiState.asStateFlow()

    @AssistedFactory
    interface Factory {
        fun create(navKey: SearchNavKey): SearchViewModel
    }
}


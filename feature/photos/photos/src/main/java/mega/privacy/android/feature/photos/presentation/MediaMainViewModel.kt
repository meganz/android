package mega.privacy.android.feature.photos.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MediaMainViewModel @Inject constructor() : ViewModel() {
    internal val uiState: StateFlow<MediaMainUiState>
        field = MutableStateFlow(MediaMainUiState())

    fun showNewAlbumDialog() {
        uiState.update {
            it.copy(newAlbumDialogEvent = triggered)
        }
    }

    fun resetNewAlbumDialog() {
        uiState.update {
            it.copy(newAlbumDialogEvent = consumed)
        }
    }
}
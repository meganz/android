package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.chat.FileGalleryItem
import mega.privacy.android.domain.usecase.GetAllGalleryImages
import mega.privacy.android.domain.usecase.GetAllGalleryVideos
import timber.log.Timber
import javax.inject.Inject

/**
 * Chat gallery view model
 *
 */
@HiltViewModel
class ChatGalleryViewModel @Inject constructor(
    private val getAllGalleryImages: GetAllGalleryImages,
    private val getAllGalleryVideos: GetAllGalleryVideos,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatGalleryState())

    /**
     * State
     */
    val state = _state.asStateFlow()

    private var loadImageJob: Job? = null

    /**
     * Load gallery images
     *
     */
    fun loadGalleryImages() {
        loadImageJob?.cancel()
        loadImageJob = viewModelScope.launch {
            merge(
                getAllGalleryImages(),
                getAllGalleryVideos()
            ).onStart {
                _state.update { it.copy(isLoading = true, items = emptyList()) }
            }.catch {
                Timber.e(it)
            }.onCompletion {
                _state.update { it.copy(isLoading = false) }
            }.collect { galleryItem ->
                val newItems = _state.value.items.toMutableList()
                newItems.add(galleryItem)
                _state.update { it.copy(items = newItems.sortedByDescending { it.dateAdded }) }
            }
        }
    }

    /**
     * Remove gallery images
     *
     */
    fun removeGalleryImages() {
        loadImageJob?.cancel()
        _state.update { it.copy(items = emptyList(), isLoading = false) }
    }
}

/**
 * Chat gallery state
 *
 * @property items        Gallery items
 * @property isLoading  Is completed
 */
data class ChatGalleryState(
    val items: List<FileGalleryItem> = emptyList(),
    val isLoading: Boolean = false,
)

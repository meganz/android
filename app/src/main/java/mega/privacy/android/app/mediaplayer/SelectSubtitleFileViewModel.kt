package mega.privacy.android.app.mediaplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.mediaplayer.mapper.SubtitleFileInfoItemMapper
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.core.ui.controls.SearchWidgetState
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.usecase.mediaplayer.GetSRTSubtitleFileListUseCase
import javax.inject.Inject

/**
 * The view model for select subtitle file
 */
@HiltViewModel
class SelectSubtitleFileViewModel @Inject constructor(
    private val getSRTSubtitleFileListUseCase: GetSRTSubtitleFileListUseCase,
    private val subtitleFileInfoItemMapper: SubtitleFileInfoItemMapper,
) : ViewModel() {
    /**
     * The state for updating the subtitle file info item list
     */
    var state by mutableStateOf<List<SubtitleFileInfoItem>>(emptyList())
        private set

    private val query = MutableStateFlow<String?>(null)

    private val selected = MutableStateFlow<SubtitleFileInfo?>(null)

    /**
     * The state for updating search widget
     */
    var searchState by mutableStateOf(SearchWidgetState.COLLAPSED)
        private set

    init {
        viewModelScope.launch {
            combine(
                query,
                selected,
                getSubtitleFileInfoList(),
                ::mapToSubtitleFileInfoItem
            ).collectLatest {
                state = it
            }
        }
    }

    private fun mapToSubtitleFileInfoItem(
        search: String?,
        selected: SubtitleFileInfo?,
        subtitleFileInfoList: List<SubtitleFileInfo>,
    ): List<SubtitleFileInfoItem> =
        if (subtitleFileInfoList.isEmpty()) {
            emptyList()
        } else {
            subtitleFileInfoList.filter { info ->
                search == null || info.name.contains(search, ignoreCase = true)
            }.map { info ->
                subtitleFileInfoItemMapper(
                    isSelected = info.id == selected?.id,
                    subtitleFileInfo = info
                )
            }
        }

    /**
     * Get subtitle file info list
     *
     * @return subtitle file info list
     */
    suspend fun getSubtitleFileInfoList() =
        flowOf(getSRTSubtitleFileListUseCase())

    /**
     * Update when the item is clicked
     *
     * @param subtitleFileInfo the selected [SubtitleFileInfo]
     */
    fun itemClickedUpdate(subtitleFileInfo: SubtitleFileInfo) {
        selected.update {
            if (subtitleFileInfo.id == selected.value?.id) {
                null
            } else {
                subtitleFileInfo
            }
        }
    }

    /**
     * Filter the items that matches the query
     * @param queryString search query
     */
    fun searchQuery(queryString: String) {
        query.update { queryString }
    }

    /**
     * Get query state flow
     */
    fun getQueryStateFlow() = query

    /**
     * Get current selected subtitle file info
     */
    fun getSelectedSubtitleFileInfo() = selected.value


    /**
     * Update for search widget state
     */
    fun searchWidgetStateUpdate() {
        searchState = if (searchState == SearchWidgetState.COLLAPSED) {
            SearchWidgetState.EXPANDED
        } else {
            SearchWidgetState.COLLAPSED
        }
    }

    /**
     * Close search
     */
    fun closeSearch() {
        query.update { null }
        searchState = SearchWidgetState.COLLAPSED
    }
}
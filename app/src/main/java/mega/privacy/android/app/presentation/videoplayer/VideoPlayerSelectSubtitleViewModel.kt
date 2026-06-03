package mega.privacy.android.app.presentation.videoplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.mediaplayer.mapper.SubtitleFileInfoItemMapper
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerSubtitleUiState
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetSRTSubtitleFileListUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.core.coroutine.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the subtitle selection screen in the revamped video player.
 *
 * Uses [MonitorHiddenNodesEnabledUseCase] to determine account eligibility for hidden nodes,
 * replacing the raw [mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase] +
 * [mega.privacy.android.domain.usecase.GetBusinessStatusUseCase] combination used in the legacy player.
 */
@HiltViewModel
internal class VideoPlayerSelectSubtitleViewModel @Inject constructor(
    private val getSRTSubtitleFileListUseCase: GetSRTSubtitleFileListUseCase,
    private val subtitleFileInfoItemMapper: SubtitleFileInfoItemMapper,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
) : ViewModel() {

    private val subtitleFileListFlow = MutableStateFlow(emptyList<SubtitleFileInfo>())
    private val selectedFlow = MutableStateFlow<SubtitleFileInfo?>(null)
    private val queryFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VideoPlayerSubtitleUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            subtitleFileListFlow,
            selectedFlow,
            queryFlow,
            monitorHiddenNodesEnabledUseCase(),
            monitorShowHiddenItemsUseCase(),
        ) { subtitleFileList, selected, query, hiddenNodesEnabled, showHiddenItems ->
            val filteredItems = filterItems(subtitleFileList, hiddenNodesEnabled, showHiddenItems)
            val mappedItems = mapItems(filteredItems, selected, query)
            VideoPlayerSubtitleUiState(
                isLoading = false,
                items = mappedItems,
                hiddenNodesEnabled = hiddenNodesEnabled,
                query = query,
                selectedSubtitleFileInfo = selected,
            )
        }.catch { e ->
            Timber.e(e, "Failed to load subtitle files")
        }.asUiStateFlow(viewModelScope, VideoPlayerSubtitleUiState())
    }

    /**
     * Loads the subtitle file list from the repository.
     */
    suspend fun getSubtitleFileInfoList() {
        subtitleFileListFlow.update { getSRTSubtitleFileListUseCase() }
    }

    /**
     * Selects or deselects the given subtitle file.
     * If the item is already selected, it is deselected.
     */
    fun itemClickedUpdate(subtitleFileInfo: SubtitleFileInfo) {
        selectedFlow.update {
            if (subtitleFileInfo.id == selectedFlow.value?.id) null else subtitleFileInfo
        }
    }

    /**
     * Clears the current subtitle selection.
     */
    internal fun clearSelectedItem() {
        selectedFlow.update { null }
    }

    /**
     * Filters the displayed items by the given query string.
     */
    fun searchQuery(queryString: String) {
        queryFlow.update { queryString }
    }

    private fun filterItems(
        items: List<SubtitleFileInfo>,
        hiddenNodesEnabled: Boolean,
        showHiddenItems: Boolean,
    ): List<SubtitleFileInfo> =
        if (showHiddenItems || !hiddenNodesEnabled) {
            items
        } else {
            items.filter { !it.isMarkedSensitive && !it.isSensitiveInherited }
        }

    private fun mapItems(
        items: List<SubtitleFileInfo>,
        selected: SubtitleFileInfo?,
        query: String?,
    ): List<SubtitleFileInfoItem> =
        items.filter { info ->
            query == null || info.name.contains(query, ignoreCase = true)
        }.map { info ->
            subtitleFileInfoItemMapper(
                isSelected = info.id == selected?.id,
                subtitleFileInfo = info,
            )
        }
}

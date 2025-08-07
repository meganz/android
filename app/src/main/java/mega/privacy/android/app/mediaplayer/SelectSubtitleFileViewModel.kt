package mega.privacy.android.app.mediaplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.mediaplayer.mapper.SubtitleFileInfoItemMapper
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.app.mediaplayer.model.SubtitleLoadState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetSRTSubtitleFileListUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.mobile.analytics.event.SearchModeEnablePressedEvent
import javax.inject.Inject

/**
 * The view model for select subtitle file
 */
@HiltViewModel
class SelectSubtitleFileViewModel @Inject constructor(
    private val getSRTSubtitleFileListUseCase: GetSRTSubtitleFileListUseCase,
    private val subtitleFileInfoItemMapper: SubtitleFileInfoItemMapper,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<SubtitleLoadState>(SubtitleLoadState.Loading)

    /**
     * The state for updating the subtitle file info item list
     */
    val state = _state.asStateFlow()

    private val query = MutableStateFlow<String?>(null)

    private val selected = MutableStateFlow<SubtitleFileInfo?>(null)

    private val subtitleFileListState = MutableStateFlow(emptyList<SubtitleFileInfo>())

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
                subtitleFileListState,
                monitorAccountDetailUseCase(),
                monitorShowHiddenItemsUseCase()
            ) { search, selected, subtitleFileInfoList, accountDetail, showHiddenItems ->
                val accountType = accountDetail.levelDetail?.accountType
                val businessStatus =
                    if (accountType?.isBusinessAccount == true) {
                        getBusinessStatusUseCase()
                    } else null

                val isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired

                val filteredItems =
                    filterNonSensitiveItems(
                        items = subtitleFileInfoList,
                        accountType = accountType,
                        showHiddenItems = showHiddenItems,
                        isBusinessAccountExpired = isBusinessAccountExpired,
                    )
                Triple(
                    mapToSubtitleFileInfoItem(
                        search,
                        selected,
                        filteredItems,
                    ), accountType, isBusinessAccountExpired
                )
            }.collect { (list, accountType, isBusinessAccountExpired) ->
                _state.update {
                    if (list.isEmpty()) {
                        SubtitleLoadState.Empty
                    } else {
                        SubtitleLoadState.Success(
                            items = list,
                            accountType = accountType,
                            isBusinessAccountExpired = isBusinessAccountExpired,
                        )
                    }
                }
            }
        }
    }

    private fun mapToSubtitleFileInfoItem(
        search: String?,
        selected: SubtitleFileInfo?,
        subtitleFileInfoList: List<SubtitleFileInfo>,
    ): List<SubtitleFileInfoItem> {
        return if (subtitleFileInfoList.isEmpty()) {
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
    }

    /**
     * Get subtitle file info list
     *
     * @return subtitle file info list
     */
    suspend fun getSubtitleFileInfoList() =
        subtitleFileListState.update { getSRTSubtitleFileListUseCase() }

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

    internal fun clearSelectedItem() {
        selected.update { null }
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
     * Get current selected subtitle file info flow
     */
    fun getSelectedSubtitleFileInfoFlow() = selected


    /**
     * Update for search widget state
     */
    fun searchWidgetStateUpdate() {
        searchState = if (searchState == SearchWidgetState.COLLAPSED) {
            Analytics.tracker.trackEvent(SearchModeEnablePressedEvent)
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

    private fun filterNonSensitiveItems(
        items: List<SubtitleFileInfo>,
        accountType: AccountType?,
        showHiddenItems: Boolean,
        isBusinessAccountExpired: Boolean,
    ): List<SubtitleFileInfo> {
        accountType ?: return items
        return if (showHiddenItems || !accountType.isPaid || isBusinessAccountExpired) {
            items
        } else {
            items.filter { !it.isMarkedSensitive && !it.isSensitiveInherited }
        }
    }
}
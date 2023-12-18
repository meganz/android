package mega.privacy.android.app.presentation.audiosection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.app.presentation.audiosection.mapper.UIAudioMapper
import mega.privacy.android.app.presentation.audiosection.model.AudioSectionState
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.audiosection.GetAllAudioUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model for audio section
 */
@HiltViewModel
class AudioSectionViewModel @Inject constructor(
    private val getAllAudioUseCase: GetAllAudioUseCase,
    private val uiAudioMapper: UIAudioMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AudioSectionState())

    /**
     * The state regarding the business logic
     */
    val state: StateFlow<AudioSectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            merge(
                monitorNodeUpdatesUseCase(),
                monitorOfflineNodeUpdatesUseCase()
            ).conflate()
                .catch {
                    Timber.e(it)
                }.collect {
                    refreshNodes()
                }
        }
    }

    internal fun refreshNodes() = viewModelScope.launch {
        val audioList = getUIAudioList()
        val sortOrder = getCloudSortOrder()
        _state.update {
            it.copy(
                allAudios = audioList,
                sortOrder = sortOrder,
                progressBarShowing = false,
                scrollToTop = false
            )
        }
    }

    private fun getUIAudioList() = getAllAudioUseCase().map { uiAudioMapper(it) }

    internal fun refreshWhenOrderChanged() =
        viewModelScope.launch {
            _state.update {
                it.copy(
                    sortOrder = getCloudSortOrder(),
                    progressBarShowing = true
                )
            }
            refreshNodes()
        }
}
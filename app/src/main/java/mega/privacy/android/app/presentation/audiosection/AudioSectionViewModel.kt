package mega.privacy.android.app.presentation.audiosection

import android.content.Intent
import android.net.Uri
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
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.app.presentation.audiosection.mapper.UIAudioMapper
import mega.privacy.android.app.presentation.audiosection.model.AudioSectionState
import mega.privacy.android.app.presentation.audiosection.model.UIAudio
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER
import mega.privacy.android.app.utils.FileUtil.getDownloadLocation
import mega.privacy.android.app.utils.FileUtil.getLocalFile
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.audiosection.GetAllAudioUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import timber.log.Timber
import java.io.File
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
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val getFileUrlByNodeHandleUseCase: GetFileUrlByNodeHandleUseCase,
    private val setViewType: SetViewType,
    private val monitorViewType: MonitorViewType,
) : ViewModel() {
    private val _state = MutableStateFlow(AudioSectionState())

    /**
     * The state regarding the business logic
     */
    val state: StateFlow<AudioSectionState> = _state.asStateFlow()

    private var searchQuery = ""
    private val originalData = mutableListOf<UIAudio>()

    init {
        checkViewType()
        viewModelScope.launch {
            merge(
                monitorNodeUpdatesUseCase(),
                monitorOfflineNodeUpdatesUseCase()
            ).conflate()
                .catch {
                    Timber.e(it)
                }.collect {
                    setPendingRefreshNodes()
                }
        }
    }

    private fun checkViewType() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _state.update { it.copy(currentViewType = viewType) }
            }
        }
    }

    private fun setPendingRefreshNodes() = _state.update { it.copy(isPendingRefresh = true) }

    internal fun refreshNodes() = viewModelScope.launch {
        val audioList = getUIAudioList().updateOriginalData().filterAudiosBySearchQuery()
        val sortOrder = getCloudSortOrder()
        _state.update {
            it.copy(
                allAudios = audioList,
                sortOrder = sortOrder,
                progressBarShowing = false,
                scrollToTop = false,
                isPendingRefresh = false
            )
        }
    }

    private fun List<UIAudio>.filterAudiosBySearchQuery() =
        filter { audio ->
            audio.name.contains(searchQuery, true)
        }

    private fun List<UIAudio>.updateOriginalData() = also { data ->
        if (originalData.isNotEmpty()) {
            originalData.clear()
        }
        originalData.addAll(data)
    }

    private suspend fun getUIAudioList() = getAllAudioUseCase().map { uiAudioMapper(it) }

    internal fun markHandledPendingRefresh() = _state.update { it.copy(isPendingRefresh = false) }

    internal fun refreshWhenOrderChanged() =
        viewModelScope.launch {
            val sortOrder = getCloudSortOrder()
            _state.update {
                it.copy(
                    sortOrder = sortOrder,
                    progressBarShowing = true
                )
            }
            refreshNodes()
        }

    internal fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            when (_state.value.currentViewType) {
                ViewType.LIST -> setViewType(ViewType.GRID)
                ViewType.GRID -> setViewType(ViewType.LIST)
            }
        }
    }

    /**
     * Detect the node whether is local file
     *
     * @param handle node handle
     * @return true is local file, otherwise is false
     */
    internal suspend fun isLocalFile(
        handle: Long,
    ): String? =
        getNodeByHandle(handle)?.let { node ->
            val localPath = getLocalFile(node)
            File(getDownloadLocation(), node.name).let { file ->
                if (localPath != null && ((isFileAvailable(file) && file.length() == node.size)
                            || (node.fingerprint == getFingerprintUseCase(localPath)))
                ) {
                    localPath
                } else {
                    null
                }
            }
        }

    /**
     * Update intent
     *
     * @param handle node handle
     * @param name node name
     * @param intent Intent
     * @return updated intent
     */
    internal suspend fun updateIntent(
        handle: Long,
        name: String,
        intent: Intent,
    ): Intent {
        if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            intent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }

        getFileUrlByNodeHandleUseCase(handle)?.let { url ->
            Uri.parse(url)?.let { uri ->
                intent.setDataAndType(uri, MimeTypeList.typeForName(name).type)
            }
        }

        return intent
    }

    internal fun shouldShowSearchMenu() = _state.value.allAudios.isNotEmpty()

    internal fun searchReady() {
        if (_state.value.searchMode)
            return

        _state.update { it.copy(searchMode = true) }
        searchQuery = ""
    }

    internal fun searchQuery(query: String) {
        if (searchQuery == query)
            return

        searchQuery = query
        searchNodeByQueryString()
    }

    internal fun exitSearch() {
        _state.update { it.copy(searchMode = false) }
        searchQuery = ""
        refreshNodes()
    }

    private fun searchNodeByQueryString() {
        _state.update {
            it.copy(
                allAudios = originalData.filter { audio ->
                    audio.name.contains(searchQuery, true)
                },
                scrollToTop = true
            )
        }
    }
}
package mega.privacy.android.app.presentation.documentsection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.documentsection.model.DocumentSectionUiState
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntity
import mega.privacy.android.app.presentation.documentsection.model.DocumentUiEntityMapper
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.documentsection.GetAllDocumentsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model for the document section
 */
@HiltViewModel
class DocumentSectionViewModel @Inject constructor(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase,
    private val documentUIEntityMapper: DocumentUiEntityMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorViewType: MonitorViewType,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentSectionUiState())
    internal val uiState = _uiState.asStateFlow()

    private val originalData = mutableListOf<DocumentUiEntity>()

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
                    refreshDocumentNodes()
                }
        }
    }

    internal suspend fun refreshDocumentNodes() =
        runCatching {
            getDocumentUIEntityList().updateOriginalData().filterDocumentsBySearchQuery()
        }.onSuccess { documentList ->
            val sortOrder = getCloudSortOrder()
            _uiState.update {
                it.copy(
                    allDocuments = documentList,
                    sortOrder = sortOrder,
                    isLoading = false
                )
            }
        }.onFailure {
            Timber.e(it)
        }

    private suspend fun getDocumentUIEntityList() = getAllDocumentsUseCase().map {
        documentUIEntityMapper(it)
    }

    private fun List<DocumentUiEntity>.updateOriginalData() = also { data ->
        originalData.clear()
        originalData.addAll(data)
    }

    private fun List<DocumentUiEntity>.filterDocumentsBySearchQuery() =
        filter { document ->
            _uiState.value.searchQuery?.let { query ->
                document.name.contains(query, true)
            } ?: true
        }

    private fun checkViewType() {
        viewModelScope.launch {
            monitorViewType().collect { viewType ->
                _uiState.update { it.copy(currentViewType = viewType) }
            }
        }
    }
}
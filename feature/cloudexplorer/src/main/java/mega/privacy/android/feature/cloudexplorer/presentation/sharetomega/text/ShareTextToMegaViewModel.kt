package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.file.CreateTextFileWithContentUseCase
import mega.privacy.android.shared.nodes.extension.orInvalid
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShareTextToMegaViewModel @Inject constructor(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val createTextFileWithContentUseCase: CreateTextFileWithContentUseCase,
) : ViewModel() {

    private val textFileState = MutableStateFlow(TextFileState())

    val uiState: StateFlow<ShareTextToMegaUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(rootNodeIdFlow(), textFileState) { rootNodeId, textFile ->
            ShareTextToMegaUiState.Data(
                rootNodeId = rootNodeId,
                textFile = textFile.file,
                isCreatingFile = textFile.isCreating,
                createFileFailedEvent = textFile.failedEvent,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, ShareTextToMegaUiState.Loading)
    }

    fun onTextFileConsumed() {
        textFileState.update { it.copy(file = consumed()) }
    }

    fun onCreateFileFailedEventConsumed() {
        textFileState.update { it.copy(failedEvent = consumed) }
    }

    private fun rootNodeIdFlow() = flow {
        emit(getRootNodeIdUseCase.orInvalid())
    }

    /**
     * Builds a temporary cache file containing [fileContent] under [fileName] so it can be
     * uploaded. While the file is written [ShareTextToMegaUiState.Data.isCreatingFile] is true.
     * On success, emits a `triggered` [ShareTextToMegaUiState.Data.textFile] event carrying the
     * created file; on failure (cache lookup or write error), triggers
     * [ShareTextToMegaUiState.Data.createFileFailedEvent] instead.
     *
     * @param fileName File name chosen by the user (including extension, e.g. `note.txt`).
     * @param fileContent Raw text content to write into the temporary file.
     */
    fun createTextFile(fileName: String, fileContent: String) {
        viewModelScope.launch {
            textFileState.update { it.copy(isCreating = true) }
            val file = runCatching {
                createTextFileWithContentUseCase(fileName, fileContent)
            }.onFailure {
                Timber.e(it, "Failed to create share text file")
            }.getOrNull()
            textFileState.update {
                if (file != null) {
                    it.copy(isCreating = false, file = triggered(file))
                } else {
                    it.copy(isCreating = false, failedEvent = triggered)
                }
            }
        }
    }
}

private data class TextFileState(
    val file: StateEventWithContent<DocumentEntity> = consumed(),
    val isCreating: Boolean = false,
    val failedEvent: StateEvent = consumed,
)

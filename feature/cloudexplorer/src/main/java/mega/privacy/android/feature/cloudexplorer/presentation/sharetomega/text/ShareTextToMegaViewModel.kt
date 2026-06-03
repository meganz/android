package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.file.CreateTextFileWithContentUseCase
import mega.privacy.android.core.coroutine.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShareTextToMegaViewModel @Inject constructor(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val createTextFileWithContentUseCase: CreateTextFileWithContentUseCase,
) : ViewModel() {

    private val fileUriChannel = Channel<StateEventWithContent<UriPath>>(Channel.BUFFERED)

    val uiState: StateFlow<ShareTextToMegaUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            rootNodeIdFlow(),
            fileUriChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
        ) { rootNodeId, fileUri ->
            ShareTextToMegaUiState.Data(
                rootNodeId = rootNodeId,
                fileUri = fileUri,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, ShareTextToMegaUiState.Loading)
    }

    fun onFileUriConsumed() {
        fileUriChannel.trySend(consumed())
    }

    private fun rootNodeIdFlow() = flow {
        emit(
            runCatching { getRootNodeIdUseCase() }
                .onFailure { Timber.e(it) }
                .getOrNull() ?: NodeId(-1)
        )
    }

    /**
     * Builds a temporary cache file containing [fileContent] under [fileName] so it can be
     * uploaded. On success, emits a `triggered` [ShareTextToMegaUiState.Data.fileUri] event
     * carrying the cache file's [UriPath]; on failure (cache lookup or write error), no event
     * is emitted and the consumer remains in the previous state.
     *
     * @param fileName File name chosen by the user (including extension, e.g. `note.txt`).
     * @param fileContent Raw text content to write into the temporary file.
     */
    fun createTextFile(fileName: String, fileContent: String) {
        viewModelScope.launch {
            val uri = runCatching {
                createTextFileWithContentUseCase(fileName, fileContent)
            }.onFailure {
                Timber.e(it, "Failed to create share text file")
            }.getOrNull()
            uri?.let {
                fileUriChannel.send(triggered(uri))
            }
        }
    }
}

package mega.privacy.android.shared.nodes.dialog.newfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.NodeNameException
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.file.IsValidTextFileUseCase
import mega.privacy.android.domain.usecase.node.ValidateNodeNameUseCase
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialogUiState.Companion.DEFAULT_LINK_FILE_EXTENSION
import timber.log.Timber

/**
 * ViewModel for the new text file dialog. Owns the file name input state, runs validation
 * and exposes a one-shot event so the caller can perform the positive action.
 */
@HiltViewModel(assistedFactory = NewTextFileNodeDialogViewModel.Factory::class)
class NewTextFileNodeDialogViewModel @AssistedInject constructor(
    private val validateNodeNameUseCase: ValidateNodeNameUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val isValidTextFileUseCase: IsValidTextFileUseCase,
    @Assisted private val args: Args,
) : ViewModel() {

    private val fileNameChannel = Channel<String>(Channel.CONFLATED)
    private val fileNameExceptionChannel = Channel<NodeNameException?>(Channel.CONFLATED)
    private val validationSuccessEventChannel =
        Channel<StateEventWithContent<String>>(Channel.BUFFERED)

    val uiState: StateFlow<NewTextFileNodeDialogUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            fileNameChannel.receiveAsFlow()
                .onStart { emit(args.defaultExtension) },
            fileNameExceptionChannel.receiveAsFlow()
                .onStart { emit(null) },
            validationSuccessEventChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
        ) { fileName, fileNameException, validationSuccessEvent ->
            NewTextFileNodeDialogUiState.Data(
                parentNodeId = args.parentNodeId,
                fileName = fileName,
                fileNameException = fileNameException,
                validationSuccessEvent = validationSuccessEvent,
            )
        }.asUiStateFlow(
            viewModelScope,
            NewTextFileNodeDialogUiState.Loading,
        )
    }

    /**
     * Updates the file name input. Clears any previous validation error.
     */
    fun onFileNameChanged(fileName: String) {
        fileNameChannel.trySend(fileName)
        fileNameExceptionChannel.trySend(null)
    }

    /**
     * Validates the current file name against the parent node. On failure updates the
     * [NewTextFileNodeDialogUiState.fileNameException]; on success triggers
     * [NewTextFileNodeDialogUiState.validationSuccessEvent] with the trimmed file name.
     */
    fun validateFileName() {
        (uiState.value as? NewTextFileNodeDialogUiState.Data)?.let { state ->
            viewModelScope.launch {
                runCatching {
                    val trimmedFileName = state.fileName.trim()
                    val parentOrRootNodeId = if (state.parentNodeId.longValue != -1L) {
                        state.parentNodeId
                    } else {
                        getRootNodeUseCase()?.id
                            ?: throw IllegalStateException("Root node not found")
                    }
                    validateNodeNameUseCase(trimmedFileName, parentOrRootNodeId)
                    val hasExtension = trimmedFileName.substringAfterLast('.', "").isNotEmpty()
                    if (hasExtension && !trimmedFileName.endsWith(DEFAULT_LINK_FILE_EXTENSION)) {
                        isValidTextFileUseCase(trimmedFileName)
                    }
                    trimmedFileName
                }.onSuccess { fileName ->
                    validationSuccessEventChannel.send(triggered(fileName))
                }.onFailure { e ->
                    when (e) {
                        is NodeNameException -> fileNameExceptionChannel.send(e)
                        else -> Timber.e(e, "Failed to validate new text file name")
                    }
                }
            }
        }
    }

    /**
     * Marks [NewTextFileNodeDialogUiState.validationSuccessEvent] as consumed.
     */
    fun onValidationSuccessEventConsumed() {
        validationSuccessEventChannel.trySend(consumed())
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): NewTextFileNodeDialogViewModel
    }

    /**
     * @property parentNodeId Parent folder where the new file will live, or `NodeId(-1L)` to
     *  fall back to the user's root cloud node at validation time.
     * @property defaultExtension Pre-filled extension shown in the input. Defaults to `.txt`.
     */
    data class Args(
        val parentNodeId: NodeId,
        val defaultExtension: String,
    )
}

package mega.privacy.android.app.presentation.transfers.preview.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.transfers.preview.view.navigation.FakePreviewArgs
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.transfers.NoTransferToShowException
import mega.privacy.android.domain.exception.transfers.TransferNotFoundException
import mega.privacy.android.domain.usecase.transfers.GetTransferByUniqueIdUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.previews.BroadcastTransferTagToCancelUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Fake preview view model.
 *
 * @param uiState [FakePreviewState] for ui state.
 */
@HiltViewModel
class FakePreviewViewModel @Inject constructor(
    private val getTransferByUniqueIdUseCase: GetTransferByUniqueIdUseCase,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val broadcastTransferTagToCancelUseCase: BroadcastTransferTagToCancelUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FakePreviewState())
    val uiState = _uiState.asStateFlow()

    private val fakePreviewArgs = FakePreviewArgs(savedStateHandle)

    init {
        checkArgs()
        getTransfer()
        monitorTransferEvents()
        checkTransferTagToCancel()
    }

    private fun checkArgs() {
        if (fakePreviewArgs.transferUniqueId == null && fakePreviewArgs.transferTagToCancel == null) {
            Timber.e("No transferUniqueId and no transferTagToCancel provided")
            _uiState.update { state -> state.copy(error = NoTransferToShowException()) }
        }
    }

    private fun getTransfer() {
        fakePreviewArgs.transferUniqueId?.let {
            viewModelScope.launch {
                runCatching {
                    getTransferByUniqueIdUseCase(it)
                }.getOrNull()?.let { transfer ->
                    val extension = transfer.fileName.substringAfterLast('.')
                    val fileTypeResId = fileTypeIconMapper(extension)

                    _uiState.update { state ->
                        state.copy(
                            fileName = transfer.fileName,
                            fileTypeResId = fileTypeResId,
                        )
                    }
                } ?: run {
                    fakePreviewArgs.transferPath?.let { path ->
                        if (File(path).exists()) {
                            _uiState.update { state ->
                                state.copy(
                                    progress = Progress(1f),
                                    previewFilePathToOpen = path,
                                )
                            }
                        } else {
                            null
                        }
                    } ?: run {
                        Timber.e("Transfer not found")
                        _uiState.update { state -> state.copy(error = TransferNotFoundException()) }
                    }
                }
            }
        }
    }

    private fun monitorTransferEvents() {
        fakePreviewArgs.transferUniqueId?.let {
            viewModelScope.launch {
                monitorTransferEventsUseCase()
                    .filter { event -> event.transfer.uniqueId == it }
                    .collectLatest { event ->
                        when (event) {
                            is TransferEvent.TransferUpdateEvent -> {
                                _uiState.update { state -> state.copy(progress = event.transfer.progress) }
                            }

                            is TransferEvent.TransferTemporaryErrorEvent -> {
                                if (event.error is QuotaExceededMegaException) {
                                    _uiState.update { state -> state.copy(error = event.error) }
                                }
                            }

                            is TransferEvent.TransferFinishEvent -> {
                                if (event.error == null) {
                                    _uiState.update { state ->
                                        state.copy(
                                            progress = Progress(1f),
                                            previewFilePathToOpen = event.transfer.localPath,
                                        )
                                    }
                                } else {
                                    val error = if (event.transfer.isCancelled) {
                                        NoTransferToShowException()
                                    } else {
                                        event.error
                                    }
                                    _uiState.update { state -> state.copy(error = error) }
                                }
                            }

                            else -> {
                                //No relevant events. Do nothing.
                            }
                        }
                    }
            }
        }
    }

    /**
     * Consume transfer event
     */
    fun consumeTransferEvent() {
        _uiState.update { state -> state.copy(transferEvent = consumed()) }
    }

    private fun checkTransferTagToCancel() {
        fakePreviewArgs.transferTagToCancel?.let {
            viewModelScope.launch {
                broadcastTransferTagToCancelUseCase(it)
            }
            Timber.d("Broadcast sent to cancel transfer with tag: $it")

            if (fakePreviewArgs.transferUniqueId == null) {
                _uiState.update { state -> state.copy(error = NoTransferToShowException()) }
            }
        }
    }
}
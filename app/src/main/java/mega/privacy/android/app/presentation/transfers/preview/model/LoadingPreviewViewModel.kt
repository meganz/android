package mega.privacy.android.app.presentation.transfers.preview.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.transfers.preview.view.LoadingPreviewInfo
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.transfers.NoTransferToShowException
import mega.privacy.android.domain.exception.transfers.TransferNotFoundException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.transfers.GetTransferByUniqueIdUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.previews.BroadcastTransferTagToCancelUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Fake preview view model.
 *
 * @property uiState [LoadingPreviewState] for ui state.
 */
@HiltViewModel
class LoadingPreviewViewModel @Inject constructor(
    private val getTransferByUniqueIdUseCase: GetTransferByUniqueIdUseCase,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val broadcastTransferTagToCancelUseCase: BroadcastTransferTagToCancelUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
    savedStateHandle: SavedStateHandle,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoadingPreviewState())
    val uiState = _uiState.asStateFlow()

    private var loadingPreviewInfo = savedStateHandle.toRoute<LoadingPreviewInfo>()

    init {
        checkArgs()
        getTransfer()
        monitorTransferEvents()
    }

    private fun checkArgs() {
        if (loadingPreviewInfo.transferUniqueId == null) {
            Timber.e("No transferUniqueId provided")
            _uiState.update { state -> state.copy(error = NoTransferToShowException()) }
        }
    }

    private fun getTransfer() {
        loadingPreviewInfo.transferUniqueId?.let {
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
                    loadingPreviewInfo.transferPath?.let { path ->
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
        loadingPreviewInfo.transferUniqueId?.let {
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

    private fun checkTransferTagToCancel(transferTagToCancel: Int) {
        viewModelScope.launch {
            broadcastTransferTagToCancelUseCase(transferTagToCancel)
        }
        Timber.d("Broadcast sent to cancel transfer with tag: $transferTagToCancel")

        if (loadingPreviewInfo.transferUniqueId == null) {
            _uiState.update { state -> state.copy(error = NoTransferToShowException()) }
        }
    }

    /**
     * Handles new intent
     */
    fun onNewIntent(transferTagToCancel: Int) {
        checkTransferTagToCancel(transferTagToCancel)
    }

    override fun onCleared() {
        super.onCleared()
        appScope.launch {
            broadcastTransferTagToCancelUseCase(null)
        }
    }
}
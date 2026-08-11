package mega.privacy.android.app.presentation.transfers.preview.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.NetworkUnavailableException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.transfers.NoTransferToShowException
import mega.privacy.android.domain.exception.transfers.TransferNotFoundException
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByUniqueIdUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.previews.BroadcastTransferTagToCancelUseCase
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import timber.log.Timber
import java.io.File

/**
 * View model of the screen shown while a file is downloaded for preview.
 *
 * @property uiState [LoadingPreviewState] for ui state.
 */
@HiltViewModel(assistedFactory = LoadingPreviewViewModel.Factory::class)
class LoadingPreviewViewModel @AssistedInject constructor(
    private val getTransferByUniqueIdUseCase: GetTransferByUniqueIdUseCase,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val broadcastTransferTagToCancelUseCase: BroadcastTransferTagToCancelUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
    @Assisted private val args: Args,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoadingPreviewState())
    val uiState = _uiState.asStateFlow()

    init {
        checkArgs()
        getTransfer()
        monitorTransferEvents()
        monitorConnectivity()

        args.transferTag?.let {
            checkTransferTagToCancel(it)
        }
    }

    private fun checkArgs() {
        if (args.transferUniqueId == null && args.transferTag == null) {
            Timber.e("No transferUniqueId provided")
            _uiState.update { state -> state.copy(error = NoTransferToShowException()) }
        }
    }

    private fun getTransfer() {
        args.transferUniqueId?.let {
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
                    args.transferPath?.let { path ->
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
        args.transferUniqueId?.let {
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
                                    updateTransferError(event.error)
                                }
                            }

                            is TransferEvent.TransferFinishEvent -> {
                                appScope.launch {
                                    broadcastTransferTagToCancelUseCase(null)
                                }
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
                                    updateTransferError(error)
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
     * Sets the error that ends the screen, except for bandwidth over quota while the quota-warning
     * upsell screen is enabled: the warning is then shown in this screen's own back stack, so
     * closing the screen would take the warning down with it. With the upsell disabled the legacy
     * over quota dialog belongs to the caller, which needs this screen to close first.
     */
    private suspend fun updateTransferError(error: Throwable?) {
        if (error is QuotaExceededMegaException && isQuotaWarningUpsellEnabled()) return
        _uiState.update { state -> state.copy(error = error) }
    }

    /**
     * Resolved once and kept: the value cannot change while the screen is open, and this runs
     * inside a [collectLatest] block, where a new event would cancel a pending lookup and drop
     * the error being handled.
     */
    private var quotaWarningUpsellEnabled: Boolean? = null

    private suspend fun isQuotaWarningUpsellEnabled(): Boolean =
        quotaWarningUpsellEnabled ?: runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.QuotaWarningUpsellScreen)
        }.onFailure { Timber.e(it) }
            .getOrDefault(false)
            .also { quotaWarningUpsellEnabled = it }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .collect { isConnected ->
                    if (!isConnected) {
                        _uiState.update { state ->
                            if (state.error == null && state.previewFilePathToOpen == null) {
                                state.copy(error = NetworkUnavailableException())
                            } else {
                                state
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

        if (args.transferUniqueId == null) {
            _uiState.update { state -> state.copy(error = NoTransferToShowException()) }
        }
    }

    /**
     * Handles new intent
     */
    fun onNewIntent(transferTagToCancel: Int) {
        checkTransferTagToCancel(transferTagToCancel)
    }

    /**
     * Arguments of the loading preview screen, taken from the intent that opened it.
     *
     * @param transferPath The path of the transfer where the file is being downloaded for preview.
     * @param transferUniqueId The unique ID of the transfer to preview.
     * @param transferTag The tag of the transfer to preview, used for cancellation.
     */
    data class Args(
        val transferPath: String? = null,
        val transferUniqueId: Long? = null,
        val transferTag: Int? = null,
    )

    /**
     * Factory for [LoadingPreviewViewModel].
     */
    @AssistedFactory
    interface Factory {

        /**
         * Create a [LoadingPreviewViewModel] for the given [args].
         */
        fun create(args: Args): LoadingPreviewViewModel
    }
}

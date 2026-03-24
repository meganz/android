package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber

@HiltViewModel(assistedFactory = ShareToMegaViewModel.Factory::class)
class ShareToMegaViewModel @AssistedInject constructor(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    @Assisted val args: Args,
) : ViewModel() {

    private val openFolderChannel = Channel<StateEventWithContent<NodeId>>(Channel.BUFFERED)
    private val uploadEventChannel =
        Channel<StateEventWithContent<TransferTriggerEvent>>(Channel.BUFFERED)

    val uiState: StateFlow<ShareToMegaUiState> by lazy {
        combine(
            openFolderChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
            uploadEventChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
        ) { openFolderEvent, uploadEvent ->
            ShareToMegaUiState.Data(
                rootNodeId = getRootNodeIdUseCase() ?: NodeId(-1),
                openFolderEvent = openFolderEvent,
                uploadEvent = uploadEvent,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, ShareToMegaUiState.Loading)
    }

    fun upload(nodeId: NodeId) {
        TransferTriggerEvent.StartUpload.Files(
            pathsAndNames = args.shareUris.associate { it.value to null },
            destinationId = nodeId,
            waitNotificationPermissionResponseToStart = true,
            pitagTrigger = PitagTrigger.ShareFromApp,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): ShareToMegaViewModel
    }

    data class Args(
        val shareUris: List<UriPath>,
    )
}
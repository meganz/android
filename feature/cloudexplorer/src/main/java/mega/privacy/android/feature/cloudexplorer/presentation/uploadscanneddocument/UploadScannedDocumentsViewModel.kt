package mega.privacy.android.feature.cloudexplorer.presentation.uploadscanneddocument

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.core.coroutine.asUiStateFlow
import timber.log.Timber

@HiltViewModel(assistedFactory = UploadScannedDocumentsViewModel.Factory::class)
internal class UploadScannedDocumentsViewModel @AssistedInject constructor(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    @Assisted val args: Args,
) : ViewModel() {

    val uiState: StateFlow<UploadScannedDocumentsUiState> by lazy(LazyThreadSafetyMode.NONE) {
        flow {
            emit(
                runCatching { getRootNodeIdUseCase() }
                    .onFailure { Timber.e(it) }
                    .getOrNull() ?: NodeId(-1)
            )
        }.map { rootNodeId ->
            UploadScannedDocumentsUiState.Data(
                rootNodeId = rootNodeId,
                uriPath = args.uriPath,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, UploadScannedDocumentsUiState.Loading)
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): UploadScannedDocumentsViewModel
    }

    data class Args(
        val uriPath: UriPath,
    )
}

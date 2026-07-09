package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.file.FilePrepareUseCase
import mega.privacy.android.shared.nodes.extension.orInvalid
import timber.log.Timber

@HiltViewModel(assistedFactory = ShareFilesToMegaViewModel.Factory::class)
class ShareFilesToMegaViewModel @AssistedInject constructor(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val filePrepareUseCase: FilePrepareUseCase,
    @Assisted val args: Args,
) : ViewModel() {

    val uiState: StateFlow<ShareFilesToMegaUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            flow { emit(getRootNodeIdUseCase.orInvalid()) },
            flow { emit(filePrepareUseCase(args.shareUris).isEmpty()) },
        ) { rootNodeId, hasNoFilesToUpload ->
            ShareFilesToMegaUiState.Data(
                rootNodeId = rootNodeId,
                shareUris = args.shareUris,
                hasNoFilesToUpload = hasNoFilesToUpload,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, ShareFilesToMegaUiState.Loading)
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): ShareFilesToMegaViewModel
    }

    data class Args(
        val shareUris: List<UriPath>,
    )
}
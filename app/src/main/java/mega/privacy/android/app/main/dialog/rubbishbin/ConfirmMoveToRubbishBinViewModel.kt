package mega.privacy.android.app.main.dialog.rubbishbin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ConfirmMoveToRubbishBinViewModel @Inject constructor(
    private val isNodeInRubbish: IsNodeInRubbish,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(ConfirmMoveToRubbishBinUiState())
    val state = _state.asStateFlow()

    val handles =
        savedStateHandle.get<LongArray>(ConfirmMoveToRubbishBinDialogFragment.EXTRA_HANDLES)
            ?.toList().orEmpty()

    init {
        viewModelScope.launch {
            val handle = handles.firstOrNull() ?: return@launch

            merge(
                flow { emit(isCameraUploadsPrimaryFolderEnabled(handle)) }
                    .map { isCameraUploadsPrimaryFolderEnabled ->
                        { state: ConfirmMoveToRubbishBinUiState ->
                            state.copy(
                                isCameraUploadsPrimaryNodeHandle = isCameraUploadsPrimaryFolderEnabled
                            )
                        }
                    },
                flow { emit(isCameraUploadsSecondaryFolderEnabled(handle)) }
                    .map { isCameraUploadsSecondaryFolderEnabled ->
                        { state: ConfirmMoveToRubbishBinUiState ->
                            state.copy(
                                isCameraUploadsSecondaryNodeHandle = isCameraUploadsSecondaryFolderEnabled
                            )
                        }
                    },
                flow { emit(isNodeInRubbishBin(handle)) }
                    .map { isNodeInRubbish ->
                        { state: ConfirmMoveToRubbishBinUiState ->
                            state.copy(isNodeInRubbish = isNodeInRubbish)
                        }
                    }
            ).catch {
                Timber.e(it)
            }.collect {
                _state.update(it)
            }
        }
    }

    private suspend fun isCameraUploadsPrimaryFolderEnabled(handle: Long): Boolean =
        runCatching { isCameraUploadsEnabledUseCase() && getPrimarySyncHandleUseCase() == handle }
            .getOrDefault(false)

    private suspend fun isCameraUploadsSecondaryFolderEnabled(handle: Long): Boolean =
        runCatching { isSecondaryFolderEnabled() && getSecondarySyncHandleUseCase() == handle }
            .getOrDefault(false)

    private suspend fun isNodeInRubbishBin(handle: Long): Boolean =
        runCatching { isNodeInRubbish(handle) }
            .getOrDefault(false)
}

internal data class ConfirmMoveToRubbishBinUiState(
    val isNodeInRubbish: Boolean = false,
    val isCameraUploadsPrimaryNodeHandle: Boolean = false,
    val isCameraUploadsSecondaryNodeHandle: Boolean = false,
)

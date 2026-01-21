package mega.privacy.android.core.nodecomponents.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import mega.privacy.android.domain.usecase.file.FilePrepareUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UploadFileViewModel @Inject constructor(
    private val filePrepareUseCase: FilePrepareUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val checkFileNameCollisionsUseCase: CheckFileNameCollisionsUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UploadFileUiState())
    val uiState = _uiState.asStateFlow()

    fun proceedUris(uris: List<Uri>, parentNodeId: NodeId, pitagTrigger: PitagTrigger) {
        viewModelScope.launch {
            runCatching {
                monitorStorageStateEventUseCase().value.storageState
            }.onSuccess { state ->
                if (state == StorageState.PayWall) {
                    _uiState.update {
                        it.copy(
                            overQuotaEvent = triggered
                        )
                    }
                    return@launch
                }
            }
            runCatching {
                val parentOrRootNodeId = if (parentNodeId.longValue != -1L) parentNodeId
                else getRootNodeUseCase()?.id ?: NodeId(-1L)
                val entities = filePrepareUseCase(uris.map { UriPath(it.toString()) })
                val collisions = checkFileNameCollisionsUseCase(
                    files = entities,
                    parentNodeId = parentOrRootNodeId,
                    pitagTrigger = pitagTrigger,
                )
                // resolve name collisions and keep uploading files without collisions
                if (collisions.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            nameCollisionEvent = triggered(collisions)
                        )
                    }
                }
                val collidedPaths = collisions.mapTo(mutableSetOf()) { it.path.value }
                val noCollisionPaths = entities.filter {
                    collidedPaths.contains(it.uri.value).not()
                }
                if (noCollisionPaths.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            startUploadEvent = triggered(
                                TransferTriggerEvent.StartUpload.Files(
                                    pathsAndNames = noCollisionPaths.associate { entity -> entity.uri.value to null },
                                    destinationId = parentOrRootNodeId,
                                    pitagTrigger = pitagTrigger,
                                )
                            )
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        uploadErrorEvent = triggered(e)
                    )
                }
                Timber.e(e)
            }
        }
    }

    fun onConsumeOverQuotaEvent() {
        _uiState.update { it.copy(overQuotaEvent = consumed) }
    }

    fun onConsumeNameCollisionEvent() {
        _uiState.update { it.copy(nameCollisionEvent = consumed()) }
    }

    fun onConsumeStartUploadEvent() {
        _uiState.update { it.copy(startUploadEvent = consumed()) }
    }

    fun onConsumeUploadErrorEvent() {
        _uiState.update { it.copy(uploadErrorEvent = consumed()) }
    }
}


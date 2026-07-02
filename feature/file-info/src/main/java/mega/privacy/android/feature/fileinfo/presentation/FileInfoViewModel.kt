package mega.privacy.android.feature.fileinfo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.feature.fileinfo.presentation.model.FileInfoUiState
import timber.log.Timber

@HiltViewModel(assistedFactory = FileInfoViewModel.Factory::class)
internal class FileInfoViewModel @AssistedInject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val monitorNodeUpdatesById: MonitorNodeUpdatesById,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    @Assisted private val nodeHandle: Long,
) : ViewModel() {

    private val nodeId = NodeId(nodeHandle)

    private val _uiState = MutableStateFlow(FileInfoUiState())
    val uiState: StateFlow<FileInfoUiState> = _uiState.asStateFlow()

    init {
        loadNodeInfo()
        monitorNodeUpdates()
    }

    private fun loadNodeInfo() {
        viewModelScope.launch {
            val node = runCatching { getNodeByIdUseCase(nodeId) }
                .onFailure { Timber.e(it, "Failed to load node $nodeHandle") }
                .getOrNull()

            if (node == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val accessPermissionDeferred = async {
                runCatching { getNodeAccessPermission(nodeId) }
                    .getOrNull() ?: AccessPermission.UNKNOWN
            }
            val isInRubbishDeferred = async {
                runCatching { isNodeInRubbishBinUseCase(nodeId) }.getOrDefault(false)
            }
            val isInBackupsDeferred = async {
                runCatching { isNodeInBackupsUseCase(nodeHandle) }.getOrDefault(false)
            }

            val accessPermission = accessPermissionDeferred.await()
            val isInRubbish = isInRubbishDeferred.await()
            val isInBackups = isInBackupsDeferred.await()

            val isFile: Boolean
            val sizeInBytes: Long
            val modificationTime: Long?
            when (node) {
                is TypedFileNode -> {
                    isFile = true
                    sizeInBytes = node.size
                    modificationTime = node.modificationTime
                }

                else -> {
                    isFile = false
                    sizeInBytes = 0L
                    modificationTime = null
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    title = node.name,
                    isFile = isFile,
                    sizeInBytes = sizeInBytes,
                    creationTime = node.creationTime,
                    modificationTime = modificationTime,
                    descriptionText = node.description.orEmpty(),
                    tags = node.tags.orEmpty(),
                    isTakenDown = node.isTakenDown,
                    accessPermission = accessPermission,
                    isNodeInRubbish = isInRubbish,
                    isNodeInBackups = isInBackups,
                )
            }
        }
    }

    private fun monitorNodeUpdates() {
        monitorNodeUpdatesById(nodeId)
            .catch { Timber.e(it, "Error monitoring node updates for $nodeHandle") }
            .onEach { loadNodeInfo() }
            .launchIn(viewModelScope)
    }

    @AssistedFactory
    interface Factory {
        fun create(nodeHandle: Long): FileInfoViewModel
    }
}

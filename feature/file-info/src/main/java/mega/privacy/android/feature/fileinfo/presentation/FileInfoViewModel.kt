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
import mega.privacy.android.core.nodecomponents.mapper.NodeDestinationMapper
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetAddressFromCoordinatesUseCase
import mega.privacy.android.domain.usecase.GetImageNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNodePathByIdUseCase
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.node.GetNodeLocationByIdUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.SetNodeDescriptionUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.shares.GetNodeOutSharesUseCase
import mega.privacy.android.feature.fileinfo.presentation.model.Coordinates
import mega.privacy.android.feature.fileinfo.presentation.model.FileInfoUiState
import mega.privacy.android.shared.nodes.extension.getIcon
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import timber.log.Timber

@HiltViewModel(assistedFactory = FileInfoViewModel.Factory::class)
internal class FileInfoViewModel @AssistedInject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val monitorNodeUpdatesById: MonitorNodeUpdatesById,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val getNodePathByIdUseCase: GetNodePathByIdUseCase,
    private val getNodeLocationByIdUseCase: GetNodeLocationByIdUseCase,
    private val getImageNodeByIdUseCase: GetImageNodeByIdUseCase,
    private val getAddressFromCoordinatesUseCase: GetAddressFromCoordinatesUseCase,
    private val setNodeDescriptionUseCase: SetNodeDescriptionUseCase,
    private val getNodeOutSharesUseCase: GetNodeOutSharesUseCase,
    private val nodeDestinationMapper: NodeDestinationMapper,
    @Assisted private val nodeHandle: Long,
) : ViewModel() {

    private val nodeId = NodeId(nodeHandle)

    private val _uiState = MutableStateFlow(FileInfoUiState())
    val uiState: StateFlow<FileInfoUiState> = _uiState.asStateFlow()

    init {
        loadNodeInfo()
        loadLocation()
        loadMapLocation()
        loadSharedContacts()
        monitorNodeUpdates()
    }

    private fun loadNodeInfo() {
        viewModelScope.launch {
            val node = runCatching { getNodeByIdUseCase(nodeId) }
                .onFailure { Timber.e(it, "Failed to load node $nodeHandle") }
                .getOrNull()

            if (node == null) {
                // TODO handle error state
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
            val fileTypeExtension: String?
            val thumbnailData: ThumbnailData?
            when (node) {
                is TypedFileNode -> {
                    isFile = true
                    sizeInBytes = node.size
                    modificationTime = node.modificationTime
                    fileTypeExtension = node.type.extension
                    thumbnailData = ThumbnailRequest(nodeId)
                }

                else -> {
                    isFile = false
                    sizeInBytes = 0L
                    modificationTime = null
                    fileTypeExtension = null
                    thumbnailData = null
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    title = node.name,
                    isFile = isFile,
                    iconRes = node.getIcon(fileTypeIconMapper),
                    thumbnailData = thumbnailData,
                    fileTypeExtension = fileTypeExtension,
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

    private fun loadLocation() {
        viewModelScope.launch {
            val pathDeferred = async {
                runCatching { getNodePathByIdUseCase(nodeId) }
                    .onFailure { Timber.e(it, "Failed to load path for $nodeHandle") }
                    .getOrNull()
            }
            val nodeLocation = runCatching { getNodeLocationByIdUseCase(nodeId) }
                .onFailure { Timber.e(it, "Failed to load location for $nodeHandle") }
                .getOrNull()

            val sourceType = nodeLocation?.nodeSourceType
            val folders = pathDeferred.await()
                ?.trim('/')
                ?.split('/')
                ?.dropLast(1) // the node's own name
                .orEmpty()
                .let { segments ->
                    // Incoming-share paths prefix the share root with the owner "email:".
                    if (sourceType == NodeSourceType.INCOMING_SHARES) {
                        segments.stripOwnerEmailPrefix()
                    } else {
                        segments
                    }
                }
                .filter { it.isNotBlank() }
            val destinations = nodeLocation?.let {
                runCatching { nodeDestinationMapper(it) }.getOrNull()
            }

            _uiState.update {
                it.copy(
                    nodeSourceType = sourceType,
                    locationFolders = folders,
                    locationDestinations = destinations,
                )
            }
        }
    }

    private fun loadMapLocation() {
        viewModelScope.launch {
            val coordinates = runCatching { getImageNodeByIdUseCase(nodeId) }
                .onFailure { Timber.e(it, "Failed to load image node for $nodeHandle") }
                .getOrNull()
                ?.takeIf { it.type is ImageFileTypeInfo }
                ?.let { Coordinates.createOrNull(latitude = it.latitude, longitude = it.longitude) }
            _uiState.update { it.copy(coordinates = coordinates) }

            val caption = coordinates?.let {
                runCatching { getAddressFromCoordinatesUseCase(it.latitude, it.longitude) }
                    .onFailure { e -> Timber.e(e, "Failed to resolve address for $nodeHandle") }
                    .getOrNull()
            }

            _uiState.update { it.copy(locationCaption = caption) }
        }
    }

    private fun loadSharedContacts() {
        viewModelScope.launch {
            val count = runCatching { getNodeOutSharesUseCase(nodeId) }
                .onFailure { Timber.e(it, "Failed to load out-shares for $nodeHandle") }
                .getOrNull()
                ?.size ?: 0
            _uiState.update { it.copy(sharedContactCount = count) }
        }
    }

    private fun monitorNodeUpdates() {
        monitorNodeUpdatesById(nodeId)
            .catch { Timber.e(it, "Error monitoring node updates for $nodeHandle") }
            .onEach {
                loadNodeInfo()
                loadLocation()
                loadMapLocation()
                loadSharedContacts()
            }
            .launchIn(viewModelScope)
    }

    private fun List<String>.stripOwnerEmailPrefix(): List<String> {
        val firstSegment = firstOrNull() ?: return this
        return listOf(firstSegment.substringAfter(':', missingDelimiterValue = firstSegment)) +
                drop(1)
    }

    /**
     * Creates, updates, or clears (empty string) the node description. The change is reflected back
     * in state through [monitorNodeUpdates].
     */
    fun updateDescription(description: String) {
        viewModelScope.launch {
            runCatching { setNodeDescriptionUseCase(nodeId, description) }
                .onFailure { Timber.e(it, "Failed to update description for $nodeHandle") }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(nodeHandle: Long): FileInfoViewModel
    }
}

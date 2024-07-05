package mega.privacy.android.app.presentation.offline.action

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeContentUriIntentMapper
import mega.privacy.android.app.presentation.offline.action.model.OfflineNodeActionUiEntity
import mega.privacy.android.app.presentation.offline.action.model.OfflineNodeActionsUiState
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.usecase.GetPathFromNodeContentUseCase
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.node.ExportNodesUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFilesUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Reusable ViewModel to handle offline node related actions
 */
@HiltViewModel
class OfflineNodeActionsViewModel @Inject constructor(
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val getOfflineFilesUseCase: GetOfflineFilesUseCase,
    private val exportNodesUseCase: ExportNodesUseCase,
    private val getPathFromNodeContentUseCase: GetPathFromNodeContentUseCase,
    private val snackBarHandler: SnackBarHandler,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineNodeActionsUiState())

    /**
     * Immutable UI State
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Handle share action of offline nodes
     */
    fun handleShareOfflineNodes(nodes: List<OfflineFileInformation>, isOnline: Boolean) {
        viewModelScope.launch {
            val isAllFiles = !nodes.any { it.isFolder }
            if (isAllFiles) {
                shareFiles(nodes)
            } else {
                if (isOnline) {
                    shareNodes(nodes)
                } else {
                    snackBarHandler.postSnackbarMessage(R.string.error_server_connection_problem)
                }
            }
        }
    }

    private suspend fun shareFiles(nodes: List<OfflineFileInformation>) {
        runCatching {
            getOfflineFilesUseCase(nodes).map { it.value }
        }.onSuccess { files ->
            _uiState.update {
                it.copy(shareFilesEvent = triggered(files))
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Consume share files event
     */
    fun onShareFilesEventConsumed() {
        _uiState.update {
            it.copy(shareFilesEvent = consumed())
        }
    }

    private suspend fun shareNodes(nodes: List<OfflineFileInformation>) {
        runCatching {
            exportNodesUseCase(nodes.map { it.handle.toLong() })
        }.onSuccess { linksMap ->
            if (linksMap.isEmpty()) return@onSuccess

            val links = if (linksMap.size == 1) {
                linksMap.values.first()
            } else {
                // Format each link in a new line
                linksMap.values.joinToString("\n\n")
            }
            _uiState.update {
                it.copy(sharesNodeLinksEvent = triggered(nodes.first().name to links))
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Consume share node links event
     */
    fun onShareNodeLinksEventConsumed() {
        _uiState.update {
            it.copy(sharesNodeLinksEvent = consumed())
        }
    }

    /**
     * Handle open offline file action
     */
    fun handleOpenOfflineFile(info: OfflineFileInformation) {
        viewModelScope.launch {
            runCatching {
                val localFile = getOfflineFileUseCase(info)
                val fileType = info.fileTypeInfo
                val nodeId = NodeId(info.handle.toLong())
                when {
                    fileType is PdfFileTypeInfo -> OfflineNodeActionUiEntity.Pdf(
                        nodeId = nodeId,
                        file = localFile,
                        mimeType = fileType.mimeType
                    )

                    fileType is ImageFileTypeInfo -> OfflineNodeActionUiEntity.Image(
                        nodeId = nodeId,
                        path = info.path
                    )

                    fileType is TextFileTypeInfo && localFile.length() <= TextFileTypeInfo.MAX_SIZE_OPENABLE_TEXT_FILE -> OfflineNodeActionUiEntity.Text(
                        file = localFile
                    )

                    fileType is VideoFileTypeInfo || fileType is AudioFileTypeInfo -> {
                        OfflineNodeActionUiEntity.AudioOrVideo(
                            nodeId = nodeId,
                            fileTypeInfo = fileType,
                            file = localFile,
                            parentId = info.parentId
                        )
                    }

                    fileType is UrlFileTypeInfo -> {
                        val path = getPathFromNodeContentUseCase(
                            NodeContentUri.LocalContentUri(localFile)
                        )
                        OfflineNodeActionUiEntity.Uri(path)
                    }

                    fileType is ZipFileTypeInfo -> OfflineNodeActionUiEntity.Zip(
                        nodeId = nodeId,
                        file = localFile
                    )

                    else -> OfflineNodeActionUiEntity.Other(
                        file = localFile,
                        mimeType = fileType?.mimeType ?: "*/*"
                    )
                }
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(openFileEvent = triggered(it))
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }


    /**
     * Handle Open With intent
     * This emits event to open the file in third-party app on device
     */
    fun handleOpenWithIntent(info: OfflineFileInformation) {
        viewModelScope.launch {
            runCatching {
                val localFile = getOfflineFileUseCase(info)
                when (info.fileTypeInfo) {
                    is UrlFileTypeInfo -> {
                        val path = getPathFromNodeContentUseCase(
                            NodeContentUri.LocalContentUri(localFile)
                        )
                        OfflineNodeActionUiEntity.Uri(path)
                    }

                    else -> OfflineNodeActionUiEntity.Other(
                        file = localFile,
                        mimeType = info.fileTypeInfo?.mimeType ?: "*/*"
                    )
                }

            }.onSuccess {
                _uiState.update { state ->
                    state.copy(openFileEvent = triggered(it))
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Consume open file event
     */
    fun onOpenFileEventConsumed() {
        _uiState.update {
            it.copy(openFileEvent = consumed())
        }
    }

    /**
     * Apply node content uri
     *
     * @param intent
     * @param localFile
     * @param mimeType
     * @param isSupported
     */
    fun applyNodeContentUri(
        intent: Intent,
        localFile: File,
        mimeType: String,
        isSupported: Boolean = true,
    ) {
        val contentUri = NodeContentUri.LocalContentUri(localFile)
        nodeContentUriIntentMapper(intent, contentUri, mimeType, isSupported)
    }
}


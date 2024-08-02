package mega.privacy.android.app.presentation.zipbrowser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.zipbrowser.mapper.ZipInfoUiEntityMapper
import mega.privacy.android.app.presentation.zipbrowser.model.ZipBrowserUiState
import mega.privacy.android.app.presentation.zipbrowser.model.ZipInfoUiEntity
import mega.privacy.android.app.presentation.zipbrowser.model.ZipItemClickedEventType
import mega.privacy.android.app.utils.Constants.EXTRA_PATH_ZIP
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.domain.usecase.zipbrowser.GetZipTreeMapUseCase
import mega.privacy.android.domain.usecase.zipbrowser.UnzipFileUseCase
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Zip browser view model
 */
@HiltViewModel
class ZipBrowserViewModel @Inject constructor(
    private val getZipTreeMapUseCase: GetZipTreeMapUseCase,
    private val zipInfoUiEntityMapper: ZipInfoUiEntityMapper,
    private val unzipFileUseCase: UnzipFileUseCase,
    private val getFileTypeInfoUseCase: GetFileTypeInfoUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private var zipNodeTree: Map<String, ZipTreeNode>? = null
    private val zipFullPath: String? = savedStateHandle[EXTRA_PATH_ZIP]
    private var unzipRootPath: String? = null
    private var zipFile: ZipFile? = null

    private val _uiState = MutableStateFlow(ZipBrowserUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            initData()
        }
    }

    private suspend fun initData() {
        zipFullPath?.let {
            unzipRootPath =
                "${zipFullPath.substring(0, zipFullPath.lastIndexOf("."))}${File.separator}"
            zipFile = runCatching {
                ZipFile(zipFullPath)
            }.recover { e ->
                Timber.e(e)
                runCatching {
                    ZipFile(zipFullPath, Charset.forName("Cp437"))
                }.onFailure { exception ->
                    Timber.e(exception)
                }.getOrNull()
            }.getOrNull()

            zipNodeTree = runCatching {
                getZipTreeMapUseCase(zipFile = zipFile)
            }.recover { e ->
                Timber.e(e)
                updateShowAlertDialog(true)
                null
            }.getOrNull() ?: return

            dataUpdated()
        }
    }

    private fun dataUpdated(zipFolderPath: String? = null, folderDepth: Int = 0) {
        zipNodeTree?.let { nodeTree ->
            if (nodeTree.isNotEmpty()) {
                var currentZipTreeNode: ZipTreeNode? = null
                val entities = if (zipFolderPath == null) {
                    nodeTree.values.filter { it.parentPath == null }
                } else {
                    currentZipTreeNode = nodeTree[zipFolderPath]
                    currentZipTreeNode?.children?.map {
                        nodeTree[it.path]
                    }
                }?.mapNotNull { zipTreeNode ->
                    zipTreeNode?.let { zipInfoUiEntityMapper(it) }
                } ?: emptyList()

                val parentFolderName = getTitle(zipFolderPath)
                _uiState.update {
                    it.copy(
                        items = entities,
                        parentFolderName = parentFolderName,
                        currentZipTreeNode = currentZipTreeNode,
                        folderDepth = folderDepth
                    )
                }
            }
        }
    }

    internal fun itemClicked(item: ZipInfoUiEntity) =
        unzipRootPath?.let { unzipPath ->
            handleItemClickedEventType(item, getItemClickedEventType(item, unzipPath))
        }

    private fun handleItemClickedEventType(
        item: ZipInfoUiEntity,
        eventType: ZipItemClickedEventType,
    ) {
        when (eventType) {
            ZipItemClickedEventType.OpenFolder -> {
                val folderDepth = _uiState.value.folderDepth.inc()
                dataUpdated(
                    zipFolderPath = item.path,
                    folderDepth = folderDepth
                )
            }

            ZipItemClickedEventType.OpenFile ->
                //If the zip file name is start with ".", it cannot be unzip. So show the alert.
                if (item.zipEntryType == ZipEntryType.Zip && item.name.startsWith(".")) {
                    Timber.e("zip file ${item.name} start with \".\" cannot unzip")
                    updateShowAlertDialog(true)
                } else {
                    _uiState.update {
                        it.copy(openedFile = item)
                    }
                }

            ZipItemClickedEventType.ZipFileNotUnpacked -> {
                viewModelScope.launch {
                    val unzipResult = runCatching {
                        unzipFileUseCase(zipFile, unzipRootPath)
                    }.recover { Timber.e(it) }.getOrNull()

                    if (unzipResult == true) {
                        itemClicked(item)
                    } else {
                        updateShowAlertDialog(true)
                    }

                    _uiState.update {
                        it.copy(showUnzipProgressBar = false)
                    }
                }
            }

            else -> {
                Timber.e("zip entry: $item does not exist")
                updateShowAlertDialog(true)
            }
        }
    }

    private fun getItemClickedEventType(
        zipInfoUiEntity: ZipInfoUiEntity,
        rootPath: String,
    ): ZipItemClickedEventType =
        when {
            zipInfoUiEntity.zipEntryType == ZipEntryType.Folder ->
                ZipItemClickedEventType.OpenFolder

            File(rootPath).exists().not() -> {
                _uiState.update {
                    it.copy(showUnzipProgressBar = true)
                }
                ZipItemClickedEventType.ZipFileNotUnpacked
            }

            File(rootPath + zipInfoUiEntity.path).exists() ->
                ZipItemClickedEventType.OpenFile

            else ->
                ZipItemClickedEventType.ZipItemNonExistent
        }

    /**
     * Get title of actionbar
     * @param folderPath current folder path
     */
    private fun getTitle(folderPath: String?) =
        (folderPath ?: zipFullPath)?.let { path ->
            "${
                if (path == zipFullPath) {
                    TITLE_ZIP
                } else ""
            }${path.split(File.separator).lastOrNull()?.removeSuffix(SUFFIX_ZIP)}"
        } ?: ""

    internal fun handleOnBackPressed() {
        val folderDepth = _uiState.value.folderDepth.dec()
        dataUpdated(
            zipFolderPath = if (_uiState.value.folderDepth == 1) {
                null
            } else {
                _uiState.value.currentZipTreeNode?.parentPath
            },
            folderDepth = folderDepth
        )
    }

    internal fun getUnzipRootPath() = unzipRootPath

    internal fun updateShowAlertDialog(value: Boolean) =
        _uiState.update { it.copy(showAlertDialog = value) }

    internal fun updateShowSnackBar(value: Boolean) =
        _uiState.update { it.copy(showSnackBar = value) }

    internal fun clearOpenedFile() = _uiState.update { it.copy(openedFile = null) }

    internal suspend fun getFileTypeInfo(file: File) = getFileTypeInfoUseCase(file)

    companion object {
        private const val TITLE_ZIP = "ZIP "
        private const val SUFFIX_ZIP = ".zip"
    }
}
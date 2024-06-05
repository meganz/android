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
import mega.privacy.android.app.utils.Constants.EXTRA_PATH_ZIP
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.domain.usecase.zipbrowser.GetZipTreeMapUseCase
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private lateinit var zipNodeTree: Map<String, ZipTreeNode>
    private val zipFullPath: String? = savedStateHandle[EXTRA_PATH_ZIP]

    private val _uiState = MutableStateFlow(ZipBrowserUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            initData()
        }
    }

    private suspend fun initData() {
        zipFullPath?.let {
            val zipFile = runCatching {
                ZipFile(zipFullPath)
            }.recover { e ->
                Timber.e(e)
                runCatching {
                    ZipFile(zipFullPath, Charset.forName("Cp437"))
                }.onFailure { exception ->
                    Timber.e(exception)
                }.getOrNull()
            }.getOrNull()

            zipNodeTree = getZipTreeMapUseCase(zipFile = zipFile)
            dataUpdated()
        }
    }

    private fun dataUpdated(zipFolderPath: String? = null, folderDepth: Int = 0) {
        if (zipNodeTree.isNotEmpty()) {
            var currentZipTreeNode: ZipTreeNode? = null
            val entities = if (zipFolderPath == null) {
                zipNodeTree.values.filter { it.parentPath == null }
            } else {
                currentZipTreeNode = zipNodeTree[zipFolderPath]
                currentZipTreeNode?.children?.map {
                    zipNodeTree[it.path]
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

    internal fun openFolder(zipFolderPath: String? = null) {
        val folderDepth = _uiState.value.folderDepth.inc()
        dataUpdated(
            zipFolderPath = zipFolderPath,
            folderDepth = folderDepth
        )
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

    companion object {
        private const val TITLE_ZIP = "ZIP "
        private const val SUFFIX_ZIP = ".zip"
    }
}
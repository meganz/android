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
            dataUpdated(zipFullPath.removeSuffix(File.separator))
        }
    }

    private fun dataUpdated(zipFolderPath: String?) {
        if (zipNodeTree.isNotEmpty() && zipFolderPath != null) {
            zipNodeTree[zipFolderPath]?.let { zipTreeNode ->
                val entities = zipTreeNode.children.map {
                    zipInfoUiEntityMapper(zipTreeNode)
                }
                val parentFolderName = getTitle(zipFolderPath)
                _uiState.update {
                    it.copy(
                        items = entities,
                        parentFolderName = parentFolderName,
                        currentZipTreeNode = zipTreeNode
                    )
                }
            }
        }
    }

    /**
     * Get title of actionbar
     * @param folderPath current folder path
     */
    private fun getTitle(folderPath: String) =
        "${TITLE_ZIP}${folderPath.split(File.separator).lastOrNull()?.removeSuffix(SUFFIX_ZIP)}"

    companion object {
        private const val TITLE_ZIP = "ZIP "
        private const val SUFFIX_ZIP = ".zip"
    }
}
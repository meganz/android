package mega.privacy.android.app.presentation.offline.view

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineNodeUIItem
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineUIState
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo

/**
 * Scaffold for the Offline Flow Screen
 */
@Composable
fun OfflineFeatureScreen(
    uiState: OfflineUIState,
    fileTypeIconMapper: FileTypeIconMapper,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState
            ) {
                items(uiState.offlineNodes) {
                    NodeListViewItem(
                        title = it.offlineNode.title,
                        subtitle = getDescription(it.offlineNode),
                        icon = if (it.offlineNode.isFolder) {
                            IconPackR.drawable.ic_folder_medium_solid
                        } else {
                            fileTypeIconMapper(getFileExtension(it.offlineNode.title))
                        },
                        thumbnailData = it.offlineNode.thumbnail
                    )
                    MegaDivider(dividerType = DividerType.BigStartPadding)
                }
            }
        }
    }
}

@Composable
private fun getDescription(offlineFileInfoUiState: OfflineFileInfoUiState): String {
    val context = LocalContext.current
    return if (offlineFileInfoUiState.isFolder) {
        offlineFileInfoUiState.folderInfo?.let { folderInfo ->
            if (folderInfo.numFolders == 0 && folderInfo.numFiles == 0) {
                stringResource(R.string.file_browser_empty_folder)
            } else if (folderInfo.numFolders == 0 && folderInfo.numFiles > 0) {
                pluralStringResource(
                    R.plurals.num_files_with_parameter,
                    folderInfo.numFiles,
                    folderInfo.numFiles
                )
            } else if (folderInfo.numFiles == 0 && folderInfo.numFolders > 0) {
                pluralStringResource(
                    R.plurals.num_folders_with_parameter,
                    folderInfo.numFolders,
                    folderInfo.numFolders
                )
            } else {
                pluralStringResource(
                    R.plurals.num_folders_num_files,
                    folderInfo.numFolders,
                    folderInfo.numFolders
                ) + pluralStringResource(
                    R.plurals.num_folders_num_files_2,
                    folderInfo.numFiles,
                    folderInfo.numFiles
                )
            }
        } ?: run {
            ""
        }
    } else {
        formatFileSize(offlineFileInfoUiState.totalSize, context)
            .plus(" · ")
            .plus(
                formatModifiedDate(
                    java.util.Locale(
                        Locale.current.language, Locale.current.region
                    ),
                    offlineFileInfoUiState.addedTime!!
                )
            )
    }
}

private fun getFileExtension(name: String) =
    name.substringAfterLast('.', "")


@CombinedThemePreviews
@Composable
private fun OfflineFeatureScreenPreview() {
    OfflineFeatureScreen(
        uiState = OfflineUIState(
            isLoading = false,
            offlineNodes = listOf(
                OfflineNodeUIItem(
                    offlineNode = OfflineFileInfoUiState(
                        title = "Some file.txt",
                        totalSize = 1234,
                        addedTime = System.currentTimeMillis(),
                    )
                ),
                OfflineNodeUIItem(
                    offlineNode = OfflineFileInfoUiState(
                        title = "Some Image.txt",
                        totalSize = 3456,
                        addedTime = System.currentTimeMillis(),
                    )
                ),
                OfflineNodeUIItem(
                    offlineNode = OfflineFileInfoUiState(
                        title = "Some file",
                        totalSize = 1234,
                        addedTime = System.currentTimeMillis(),
                        isFolder = true,
                        folderInfo = OfflineFolderInfo(numFiles = 2, numFolders = 3)
                    )
                ),
            )
        ),
        fileTypeIconMapper = FileTypeIconMapper()
    )
}
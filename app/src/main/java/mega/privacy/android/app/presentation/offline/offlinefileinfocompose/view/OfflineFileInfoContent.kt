package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.fileinfo.view.AvailableOfflineView
import mega.privacy.android.app.presentation.fileinfo.view.CreationModificationTimesView
import mega.privacy.android.app.presentation.fileinfo.view.FileInfoContentDivider
import mega.privacy.android.app.presentation.fileinfo.view.FolderContentView
import mega.privacy.android.app.presentation.fileinfo.view.NodeSizeView
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.app.utils.Util

/**
 * To be used inside OfflineFIleInfoScreen
 */
@Composable
internal fun OfflineFileInfoContent(
    uiState: OfflineFileInfoUiState,
    onRemoveFromOffline: () -> Unit,
) {

    Column {
        val paddingHorizontal = Modifier.padding(start = 72.dp, end = 16.dp)

        AvailableOfflineView(
            enabled = true,
            available = true,
            onCheckChanged = { checked ->
                if (!checked)
                    onRemoveFromOffline()
            },
            modifier = paddingHorizontal,
        )
        FileInfoContentDivider()

        Spacer(modifier = Modifier.height(10.dp))
        NodeSizeView(
            forFolder = uiState.isFolder,
            sizeString = Util.getSizeString(uiState.totalSize, LocalContext.current),
            modifier = paddingHorizontal,
        )

        uiState.folderInfo?.let {
            FolderContentView(
                numberOfFolders = it.numFolders,
                numberOfFiles = it.numFiles,
                modifier = paddingHorizontal,
            )
        }

        uiState.addedTime?.let {
            CreationModificationTimesView(
                creationTimeInSeconds = it,
                modificationTimeInSeconds = null,
                modifier = paddingHorizontal,
            )
        }
    }
}
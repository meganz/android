package mega.privacy.android.core.nodecomponents.sheet.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.bottomsheet.bottomSheetMetadata

@Serializable
data object HomeFabOptionsBottomSheetNavKey : NavKey {
    const val KEY = "HomeFabOptionsBottomSheetNavKey_extra_action"
}

enum class HomeFabOption {
    UploadFiles,
    UploadFolder,
    ScanDocument,
    Capture,
    CreateNewTextFile,
    AddNewSync,
    AddNewBackup,
    NewChat
}

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.homeFabOptionsBottomSheetNavigation(
    returnResult: (String, HomeFabOption) -> Unit,
) {
    entry<HomeFabOptionsBottomSheetNavKey>(metadata = bottomSheetMetadata()) {
        HomeFabOptionsBottomSheet(
            onUploadFilesClicked = {
                returnResult(HomeFabOptionsBottomSheetNavKey.KEY, HomeFabOption.UploadFiles)
            },
            onUploadFolderClicked = {
                returnResult(HomeFabOptionsBottomSheetNavKey.KEY, HomeFabOption.UploadFolder)
            },
            onScanDocumentClicked = {
                returnResult(HomeFabOptionsBottomSheetNavKey.KEY, HomeFabOption.ScanDocument)
            },
            onCaptureClicked = {
                returnResult(HomeFabOptionsBottomSheetNavKey.KEY, HomeFabOption.Capture)
            },
            onCreateNewTextFileClicked = {
                returnResult(HomeFabOptionsBottomSheetNavKey.KEY, HomeFabOption.CreateNewTextFile)
            },
            onAddNewSyncClicked = {
                returnResult(HomeFabOptionsBottomSheetNavKey.KEY, HomeFabOption.AddNewSync)
            },
            onAddNewBackupClicked = {
                returnResult(HomeFabOptionsBottomSheetNavKey.KEY, HomeFabOption.AddNewBackup)
            },
            onNewChatClicked = {
                returnResult(HomeFabOptionsBottomSheetNavKey.KEY, HomeFabOption.NewChat)
            },
        )
    }
}


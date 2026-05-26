package mega.privacy.android.app.presentation.documentscanner.navigation

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsView
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.SaveScannedDocumentsNavKey
import mega.privacy.android.shared.resources.R as SharedR

class SaveScannedDocumentsDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            saveScannedDocuments(
                navigate = navigationHandler::navigate,
                removeDestination = navigationHandler::remove,
            )
        }

    fun EntryProviderScope<NavKey>.saveScannedDocuments(
        navigate: (List<NavKey>) -> Unit,
        removeDestination: (NavKey) -> Unit,
    ) {
        entry<SaveScannedDocumentsNavKey> { key ->
            val resources = LocalResources.current
            val viewModel =
                hiltViewModel<SaveScannedDocumentsViewModel, SaveScannedDocumentsViewModel.Factory> { factory ->
                    factory.create(
                        SaveScannedDocumentsViewModel.Args(
                            originatedFromChat = key.originatedFromChat,
                            cloudDriveParentHandle = key.cloudDriveParentHandle
                                ?.takeIf { it != -1L },
                            pdfUri = key.scanPdfUri.toUri(),
                            soloImageUri = key.scanSoloImageUri?.toUri(),
                            fileFormat = resources.getString(
                                SharedR.string.document_scanning_default_file_name
                            ),
                        )
                    )
                }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            SaveScannedDocumentsView(
                uiState = uiState,
                onFilenameChanged = viewModel::onFilenameChanged,
                onFilenameConfirmed = viewModel::onFilenameConfirmed,
                onSaveButtonClicked = viewModel::onSaveButtonClicked,
                onScanDestinationSelected = viewModel::onScanDestinationSelected,
                onScanFileTypeSelected = viewModel::onScanFileTypeSelected,
                onSnackbarMessageConsumed = viewModel::onSnackbarMessageConsumed,
                onUploadScansEventConsumed = viewModel::onUploadScansEventConsumed,
                onBackToChat = { removeDestination(key) },
                onNavigate = navigate,
            )
        }
    }
}

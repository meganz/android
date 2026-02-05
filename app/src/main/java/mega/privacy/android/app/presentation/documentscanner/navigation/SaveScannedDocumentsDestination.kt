package mega.privacy.android.app.presentation.documentscanner.navigation

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsActivity
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsScreen
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.SaveScannedDocumentsActivityNavKey
import mega.privacy.android.navigation.destination.SaveScannedDocumentsNavKey
import mega.privacy.android.shared.resources.R

class SaveScannedDocumentsDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            saveScannedDocumentsDestination(
                onUploadToChat = { currentNavKey, uri, scanFileType, originatedFromChat, canSelectScanFileType ->
                    //TODO AND-22497: Handle upload to chat
                },
                onUploadToCloudDrive = { currentNavKey, uri, scanFileType, cloudDriveParentHandle, canSelectScanFileType ->
                    //TODO AND-22498: Handle upload to cloud
                }
            )
            saveScannedDocumentsLegacyDestination(navigationHandler::back)
        }

    /**
     * Navigation destination for SaveScannedDocumentsActivity that handles legacy navigation.
     *
     * It will be removed once FileExplorerActivity revamp is finished
     */
    fun EntryProviderScope<NavKey>.saveScannedDocumentsLegacyDestination(
        removeDestination: () -> Unit,
    ) {
        entry<SaveScannedDocumentsActivityNavKey>(
            metadata = transparentMetadata()
        ) { key ->
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                key.scanPdfUri.takeIf { it.isNotBlank() }?.let(Uri::parse)?.let { pdfUri ->
                    val soloImageUri =
                        key.scanSoloImageUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)

                    val intent = SaveScannedDocumentsActivity.getIntent(
                        context = context,
                        fromChat = key.originatedFromChat,
                        parentHandle = key.cloudDriveParentHandle,
                        pdfUri = pdfUri,
                        imageUris = soloImageUri?.let { listOf(it) } ?: emptyList(),
                    )
                    context.startActivity(intent)
                }

                // Immediately pop this destination from the back stack
                removeDestination()
            }
        }
    }

    fun EntryProviderScope<NavKey>.saveScannedDocumentsDestination(
        onUploadToChat: (currentNavKey: SaveScannedDocumentsNavKey, Uri, ScanFileType, comesFromChat: Boolean, canSelectScanFileType: Boolean) -> Unit,
        onUploadToCloudDrive: (currentNavKey: SaveScannedDocumentsNavKey, Uri, ScanFileType, defaultNodeDestination: Long, canSelectScanFileType: Boolean) -> Unit,
    ) {
        entry<SaveScannedDocumentsNavKey> { key ->
            val resource = LocalResources.current
            SaveScannedDocumentsScreen(
                viewModel = hiltViewModel<SaveScannedDocumentsViewModel, SaveScannedDocumentsViewModel.Factory> { factory ->
                    factory.create(
                        SaveScannedDocumentsViewModel.Args(
                            originatedFromChat = key.originatedFromChat,
                            cloudDriveParentHandle = key.cloudDriveParentHandle,
                            pdfUri = key.scanPdfUri.toUri(),
                            soloImageUri = key.scanSoloImageUri?.toUri(),
                            fileFormat = resource.getString(R.string.document_scanning_default_file_name)
                        )
                    )
                },
                onUploadToChat = { uri, scanFileType, originatedFromChat, canSelectScanFileType ->
                    onUploadToChat(
                        key,
                        uri,
                        scanFileType,
                        originatedFromChat,
                        canSelectScanFileType
                    )
                },
                onUploadToCloudDrive = { uri, scanFileType, cloudDriveParentHandle, canSelectScanFileType ->
                    onUploadToCloudDrive(
                        key,
                        uri,
                        scanFileType,
                        cloudDriveParentHandle,
                        canSelectScanFileType
                    )
                }
            )
        }
    }
}



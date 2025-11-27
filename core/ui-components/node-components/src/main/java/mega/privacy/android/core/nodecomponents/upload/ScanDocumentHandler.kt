package mega.privacy.android.core.nodecomponents.upload

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.resources.R as SharedR
import timber.log.Timber

/**
 * Reusable component for handling scan document functionality.
 *
 * @param parentNodeId The parent node ID where scanned documents will be saved
 * @param originatedFromChat Whether the scan originated from chat, defaults to false
 * @param megaNavigator Optional navigator, defaults to rememberMegaNavigator()
 * @param viewModel Optional ViewModel, defaults to hiltViewModel()
 */
@Composable
fun ScanDocumentHandler(
    parentNodeId: NodeId,
    originatedFromChat: Boolean = false,
    megaNavigator: MegaNavigator = rememberMegaNavigator(),
    viewModel: ScanDocumentViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scanDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.let { data ->
                with(data) {
                    val imageUris = pages?.mapNotNull { page ->
                        page.imageUri
                    } ?: emptyList()

                    // The PDF URI must exist before moving to the Scan Confirmation page
                    pdf?.uri?.let { pdfUri ->
                        megaNavigator.openSaveScannedDocumentsActivity(
                            context = context,
                            originatedFromChat = false,
                            cloudDriveParentHandle = parentNodeId.longValue,
                            scanPdfUri = pdfUri,
                            scanSoloImageUri = if (imageUris.size == 1) imageUris[0] else null,
                        )
                    } ?: run {
                        Timber.e("The PDF file could not be retrieved after scanning")
                    }
                }
            }
        } else {
            Timber.e("The ML Kit Document Scan result could not be retrieved")
        }
    }

    // Handle document scanner state changes
    LaunchedEffect(uiState.gmsDocumentScanner) {
        uiState.gmsDocumentScanner?.let { documentScanner ->
            documentScanner.apply {
                getStartScanIntent(activity!!)
                    .addOnSuccessListener {
                        scanDocumentLauncher.launch(IntentSenderRequest.Builder(it).build())
                    }
                    .addOnFailureListener { exception ->
                        Timber.e(
                            exception,
                            "An error occurred when attempting to run the ML Kit Document Scanner",
                        )
                        viewModel.onDocumentScannerFailedToOpen()
                    }
            }
            viewModel.onGmsDocumentScannerConsumed()
        }
    }

    // Show error dialog when documentScanningError is not null
    uiState.documentScanningError?.let { errorType ->
        DocumentScanningErrorDialog(
            documentScanningError = errorType,
            onErrorAcknowledged = viewModel::onDocumentScanningErrorConsumed,
            onErrorDismissed = viewModel::onDocumentScanningErrorConsumed
        )
    }
}

/**
 * Material 3 version of DocumentScanningErrorDialog using BasicDialog
 *
 * @param documentScanningError The specific Document Scanning Error
 * @param onErrorAcknowledged Lambda to execute upon clicking the confirm button
 * @param onErrorDismissed Lambda to execute upon clicking outside the Dialog bounds
 */
@Composable
private fun DocumentScanningErrorDialog(
    documentScanningError: DocumentScanningError,
    onErrorAcknowledged: () -> Unit,
    onErrorDismissed: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(SCAN_DOCUMENT_ERROR_DIALOG_TEST_TAG),
        title = stringResource(SharedR.string.document_scanning_error_dialog_title),
        description = stringResource(documentScanningError.textRes),
        positiveButtonText = stringResource(SharedR.string.document_scanning_error_dialog_confirm_button),
        onPositiveButtonClicked = onErrorAcknowledged,
        onDismiss = onErrorDismissed,
    )
}

/**
 * Test Tag for the Document Scanning Error Dialog
 */
private const val SCAN_DOCUMENT_ERROR_DIALOG_TEST_TAG =
    "scan_document_error_dialog:basic_dialog"


package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import mega.privacy.android.navigation.megaNavigator
import mega.privacy.android.shared.resources.R as SharedR
import timber.log.Timber

/**
 * Shared component for handling scan document functionality in Cloud Drive screens
 *
 * @param cloudDriveUiState Current UI state containing scanner and error information
 * @param onDocumentScannerFailedToOpen Callback when document scanner fails to open
 * @param onGmsDocumentScannerConsumed Callback when GMS document scanner is consumed
 * @param onDocumentScanningErrorConsumed Callback when document scanning error is acknowledged
 */
@Composable
fun CloudDriveScanDocumentHandler(
    cloudDriveUiState: CloudDriveUiState,
    onDocumentScannerFailedToOpen: () -> Unit,
    onGmsDocumentScannerConsumed: () -> Unit,
    onDocumentScanningErrorConsumed: () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val megaNavigator = remember {
        context.megaNavigator
    }

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
                            cloudDriveParentHandle = cloudDriveUiState.currentFolderId.longValue,
                            scanPdfUri = pdfUri,
                            scanSoloImageUri = if (imageUris.size == 1) imageUris[0] else null,
                        )
                    } ?: run {
                        Timber.e("The PDF file could not be retrieved from Cloud Drive after scanning")
                    }
                }
            }
        } else {
            Timber.e("The ML Kit Document Scan result could not be retrieved from Cloud Drive")
        }
    }

    // Handle document scanner state changes
    LaunchedEffect(cloudDriveUiState.gmsDocumentScanner) {
        cloudDriveUiState.gmsDocumentScanner?.let { documentScanner ->
            documentScanner.apply {
                getStartScanIntent(activity!!)
                    .addOnSuccessListener {
                        scanDocumentLauncher.launch(IntentSenderRequest.Builder(it).build())
                    }
                    .addOnFailureListener { exception ->
                        Timber.e(
                            exception,
                            "An error occurred when attempting to run the ML Kit Document Scanner from Cloud Drive",
                        )
                        onDocumentScannerFailedToOpen()
                    }
            }
            onGmsDocumentScannerConsumed()
        }
    }

    // Show error dialog when documentScanningError is not null
    cloudDriveUiState.documentScanningError?.let { errorType ->
        CloudDriveDocumentScanningErrorDialog(
            documentScanningError = errorType,
            onErrorAcknowledged = onDocumentScanningErrorConsumed,
            onErrorDismissed = onDocumentScanningErrorConsumed
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
private fun CloudDriveDocumentScanningErrorDialog(
    documentScanningError: DocumentScanningError,
    onErrorAcknowledged: () -> Unit,
    onErrorDismissed: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(CLOUD_DRIVE_DOCUMENT_SCANNING_ERROR_DIALOG),
        title = stringResource(SharedR.string.document_scanning_error_dialog_title),
        description = stringResource(documentScanningError.textRes),
        positiveButtonText = stringResource(SharedR.string.document_scanning_error_dialog_confirm_button),
        onPositiveButtonClicked = onErrorAcknowledged,
        onDismiss = onErrorDismissed,
    )
}

/**
 * Test Tag for the Cloud Drive Document Scanning Error Dialog
 */
private const val CLOUD_DRIVE_DOCUMENT_SCANNING_ERROR_DIALOG =
    "cloud_drive_document_scanning_error_dialog:basic_dialog" 
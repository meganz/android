package mega.privacy.android.app.presentation.documentscanner.legacy

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import mega.privacy.android.app.presentation.documentscanner.dialogs.DocumentScanningErrorDialog
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.core.passcode.rememberPasscodeAwareLauncher
import mega.privacy.android.navigation.destination.SaveScannedDocumentsNavKey
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import timber.log.Timber

/**
 * Launches the legacy ML Kit document scanner and routes its result to the
 * Save-Scanned-Documents screen. This is the legacy fallback for the continuous
 * scanner.
 *
 * Scans are saved to the Cloud Drive root (`cloudDriveParentHandle = null`) because the
 * scanner entry point (`ContinuousScanNavKey`) does not yet carry a per-screen parent
 * folder.
 *
 * @param onScanned navigate to the save screen with the scanned PDF / image.
 * @param onFinished invoked when the flow ends without a saved scan (user cancelled or
 * an error occurred) so the caller can pop this destination.
 */
@Composable
internal fun LegacyScanDocumentLauncher(
    onScanned: (SaveScannedDocumentsNavKey) -> Unit,
    onFinished: () -> Unit,
    viewModel: LegacyScanDocumentViewModel = hiltViewModel(),
) {
    val activity = LocalActivity.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var scanningError by remember { mutableStateOf<DocumentScanningError?>(null) }

    val scanLauncher = rememberPasscodeAwareLauncher(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            // User backed out of the scanner.
            onFinished()
            return@rememberPasscodeAwareLauncher
        }
        val data = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val pdfUri = data?.pdf?.uri
        if (pdfUri == null) {
            Timber.e("The PDF file could not be retrieved after scanning")
            onFinished()
        } else {
            val imageUris = data.pages?.mapNotNull { it.imageUri } ?: emptyList()
            onScanned(
                SaveScannedDocumentsNavKey(
                    originatedFromChat = false,
                    cloudDriveParentHandle = null,
                    scanPdfUri = pdfUri.toString(),
                    scanSoloImageUri = imageUris.singleOrNull()?.toString(),
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.prepareDocumentScanner()
    }

    EventEffect(
        event = uiState.scannerReadyEvent,
        onConsumed = viewModel::onScannerReadyEventConsumed,
    ) { scanner ->
        val host = activity
        if (host == null) {
            Timber.e("LegacyScanDocumentLauncher: no host Activity available to launch the scanner")
            viewModel.onDocumentScannerFailedToOpen()
            return@EventEffect
        }
        // Await the intent inside the EventEffect coroutine so the whole launch is
        // tied to this composition's lifecycle: if the destination leaves composition
        // (or the activity is recreated) before the intent resolves, the coroutine is
        // cancelled and nothing launches — instead of a stray callback firing on a
        // dead launcher. EventEffect consumes the event after this returns, so the
        // scanner launches exactly once and recomposition can't re-trigger it.
        try {
            val intentSender = scanner.getStartScanIntent(host).await()
            scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Timber.e(error, "Failed to open the legacy ML Kit document scanner")
            viewModel.onDocumentScannerFailedToOpen()
        }
    }

    EventEffect(
        event = uiState.scanningErrorEvent,
        onConsumed = viewModel::onScanningErrorEventConsumed,
    ) { error ->
        scanningError = error
    }

    DocumentScanningErrorDialog(
        documentScanningError = scanningError,
        onErrorAcknowledged = {
            scanningError = null
            onFinished()
        },
        onErrorDismissed = {
            scanningError = null
            onFinished()
        },
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        MegaCircularProgressIndicator()
    }
}

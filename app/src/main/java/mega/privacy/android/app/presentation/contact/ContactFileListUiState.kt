package mega.privacy.android.app.presentation.contact

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.documentscanner.model.DocumentScanningError
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NameCollision

/**
 * Contact file list ui state
 *
 * @property moveRequestResult
 * @property nodeNameCollisionResult
 * @property messageText
 * @property copyMoveAlertTextId
 * @property snackBarMessage
 * @property uploadEvent Event to trigger upload actions
 * @property gmsDocumentScanner The prepared ML Kit Document Scanner
 * @property documentScanningError The specific Error returned when using the modern Document Scanner
 */
data class ContactFileListUiState(
    val moveRequestResult: Result<MoveRequestResult>? = null,
    val nodeNameCollisionResult: List<NameCollision> = emptyList(),
    val messageText: String? = null,
    val copyMoveAlertTextId: Int? = null,
    val snackBarMessage: Int? = null,
    val uploadEvent: StateEventWithContent<TransferTriggerEvent.StartUpload> = consumed(),
    val gmsDocumentScanner: GmsDocumentScanner? = null,
    val documentScanningError: DocumentScanningError? = null,
)
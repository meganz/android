package mega.privacy.android.app.presentation.pdfviewer

import android.net.Uri
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.chat.ChatFile

/**
 * Pdf viewer UI state
 *
 * @property snackBarMessage                Snack bar message Id to be shown to the user
 * @property nodeMoveError                  Error when moving a node
 * @property nodeCopyError                  Error when copying a node
 * @property shouldFinishActivity           Checks if activity should be finished
 * @property nameCollision                  Name collision if identified
 * @property accountType                    the account type
 * @property isBusinessAccountExpired       if the business account is expired
 * @property isHiddenNodesOnboarded         if the user has been onboarded with hidden nodes
 * @property startChatOfflineDownloadEvent  Event to start chat node offline download
 * @property isNodeInBackups                if the node is in backups
 * @property invalidateMenuEvent            Event to invalidate options menu when node is updated
 * @property showTakenDownDialogEvent       Event to show taken down dialog when transfer is blocked
 */
data class PdfViewerState(
    val snackBarMessage: Int? = null,
    val nodeMoveError: Throwable? = null,
    val nodeCopyError: Throwable? = null,
    val shouldFinishActivity: Boolean = false,
    val nameCollision: NameCollision? = null,
    val pdfStreamData: ByteArray? = null,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val isHiddenNodesOnboarded: Boolean = false,
    val startChatOfflineDownloadEvent: StateEventWithContent<ChatFile> = consumed(),
    val isNodeInBackups: Boolean = false,
    val pdfUriData: Uri? = null,
    val lastPageViewed: Long? = null,
    val invalidateMenuEvent: StateEvent = consumed,
    val showTakenDownDialogEvent: StateEvent = consumed,
)

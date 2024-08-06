package mega.privacy.android.app.presentation.pdfviewer

import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.NameCollision

/**
 * Pdf viewer UI state
 *
 * @property snackBarMessage                Snack bar message Id to be shown to the user
 * @property nodeMoveError                  Error when moving a node
 * @property nodeCopyError                  Error when copying a node
 * @property shouldFinishActivity           Checks if activity should be finished
 * @property nameCollision                  Name collision if identified
 * @property accountType                    the account type
 * @property isHiddenNodesOnboarded         if the user has been onboarded with hidden nodes
 */
data class PdfViewerState(
    val snackBarMessage: Int? = null,
    val nodeMoveError: Throwable? = null,
    val nodeCopyError: Throwable? = null,
    val shouldFinishActivity: Boolean = false,
    val nameCollision: NameCollision? = null,
    val pdfStreamData: ByteArray? = null,
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean = false,
)

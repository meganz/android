package mega.privacy.android.app.presentation.contact

import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.domain.entity.node.MoveRequestResult

/**
 * Contact file list ui state
 *
 * @property moveRequestResult
 * @property nodeNameCollisionResult
 * @property messageText
 * @property copyMoveAlertTextId
 * @property snackBarMessage
 */
data class ContactFileListUiState(
    val moveRequestResult: Result<MoveRequestResult>? = null,
    val nodeNameCollisionResult: List<NameCollision> = emptyList(),
    val messageText: String? = null,
    val copyMoveAlertTextId: Int? = null,
    val snackBarMessage: Int? = null,
)
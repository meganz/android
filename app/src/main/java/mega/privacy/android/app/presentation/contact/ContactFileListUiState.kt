package mega.privacy.android.app.presentation.contact

import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult

/**
 * Contact file list ui state
 *
 * @property moveRequestResult
 */
data class ContactFileListUiState(
    val moveRequestResult: Result<MoveRequestResult>? = null,
    val nodeNameCollisionResult: NodeNameCollisionResult? = null,
)
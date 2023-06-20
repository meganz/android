package mega.privacy.android.app.presentation.contact

import mega.privacy.android.domain.entity.node.MoveRequestResult

/**
 * Contact file list ui state
 *
 * @property moveRequestResult
 */
data class ContactFileListUiState(
    val moveRequestResult: MoveRequestResult? = null
)
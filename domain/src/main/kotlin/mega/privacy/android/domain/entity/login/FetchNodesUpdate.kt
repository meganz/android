package mega.privacy.android.domain.entity.login

import mega.privacy.android.domain.entity.Progress

/**
 * Data class representing fetch nodes state.
 *
 * @property progress [Progress].
 * @property temporaryError String id to show as temporary error.
 */
data class FetchNodesUpdate(
    val progress: Progress? = null,
    val temporaryError: FetchNodesTemporaryError? = null,
)
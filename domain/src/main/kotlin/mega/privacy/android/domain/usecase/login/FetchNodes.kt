package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.login.FetchNodesUpdate

/**
 * Use case for fetching nodes.
 */
fun interface FetchNodes {

    /**
     * Invoke.
     *
     * @return Flow of [FetchNodesUpdate].
     */
    operator fun invoke(): Flow<FetchNodesUpdate>
}
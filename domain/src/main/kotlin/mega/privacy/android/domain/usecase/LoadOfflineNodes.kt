package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.offline.OfflineNodeInformation

/**
 * Use case for loading offline nodes
 *
 */
fun interface LoadOfflineNodes {

    /**
     * Invoke
     *
     * @param path Node path
     * @param searchQuery search query for database
     */
    suspend operator fun invoke(
        path: String,
        searchQuery: String?,
    ): List<OfflineNodeInformation>
}
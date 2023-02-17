package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.entity.FolderPair

/**
 * Returns all setup folder pairs.
 */
fun interface GetFolderPairs {

    suspend operator fun invoke(): List<FolderPair>
}

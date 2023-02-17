package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Removes all folder pairs
 */
fun interface RemoveFolderPairs {

    suspend operator fun invoke()
}

package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import javax.inject.Inject

/**
 * Returns the list of MEGA folders from users root folder.
 */
interface GetRemoteFolders {

    suspend operator fun invoke(): List<RemoteFolder>
}

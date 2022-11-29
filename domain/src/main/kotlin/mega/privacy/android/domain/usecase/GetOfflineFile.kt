package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import java.io.File

/**
 * Get offline file
 */
fun interface GetOfflineFile {
    /**
     * Invoke
     *
     * @param offlineInformation
     * @return the offline file
     */
    suspend operator fun invoke(offlineInformation: OfflineNodeInformation): File
}

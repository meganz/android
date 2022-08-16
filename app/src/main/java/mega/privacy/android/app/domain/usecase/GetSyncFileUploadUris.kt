package mega.privacy.android.app.domain.usecase

import android.net.Uri

/**
 * Get sync file upload
 *
 */
interface GetSyncFileUploadUris {

    /**
     * Invoke
     *
     * @return list of sync file upload uris
     */
    suspend operator fun invoke(): List<Uri>
}

package mega.privacy.android.data.gateway

import android.net.Uri
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia

/**
 * Camera Upload Media Files Gateway
 */
interface CameraUploadsMediaGateway {

    /**
     * Get the media queue for a given media type
     *
     * @param uri different media store file type
     * @param selectionQuery db query
     *
     * @return list of camera upload media
     */
    suspend fun getMediaList(
        uri: Uri,
        selectionQuery: String?,
    ): List<CameraUploadsMedia>

    /**
     * Get the selection query to filter the media based on the parent path
     *
     * @param parentPath path that contains the media
     */
    fun getMediaSelectionQuery(parentPath: String): String
}

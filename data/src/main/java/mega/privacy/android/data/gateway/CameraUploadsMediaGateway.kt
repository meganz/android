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
     * Update camera upload folder (node list) icon
     *
     * @param nodeHandle    updated node handle
     * @param isSecondary   if updated node handle is secondary media
     */
    suspend fun sendUpdateFolderIconBroadcast(nodeHandle: Long, isSecondary: Boolean)

    /**
     * Update camera upload folder destination in settings
     *
     * @param nodeHandle    updated node handle
     * @param isSecondary   if updated node handle is secondary media
     */
    suspend fun sendUpdateFolderDestinationBroadcast(nodeHandle: Long, isSecondary: Boolean)
}

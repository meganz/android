package mega.privacy.android.data.facade

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.CameraUploadsMediaGateway
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max

/**
 * Camera Upload Media Facade implements [CameraUploadsMediaGateway]
 */
internal class CameraUploadsMediaFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : CameraUploadsMediaGateway {

    /**
     * Debug size limitation, applied if not null
     */
    private val debugSizeLimitation: Int? = null

    override suspend fun getMediaList(
        uri: Uri,
        selectionQuery: String?,
    ): List<CameraUploadsMedia> = runCatching {
        val isVideo = uri.isVideoUri()
        createMediaCursor(uri, selectionQuery, isVideo)?.use { cursor ->
            Timber.d("Extract ${cursor.count} Media from Cursor")
            cursor.extractMedia(isVideo)
        } ?: run {
            Timber.d("Extract 0 Media - Cursor is NULL")
            emptyList()
        }
    }.getOrElse {
        Timber.e(it)
        emptyList()
    }

    override fun getMediaSelectionQuery(parentPath: String): String =
        "${MediaStore.MediaColumns.DATA} LIKE '${parentPath}%'"

    /**
     *  Return the column of the media store to retrieve data from
     *
     *  @return an array of strings representing a column of the media store
     */
    private fun getProjection(isVideo: Boolean) = arrayOf(
        if (isVideo) MediaStore.Video.Media._ID else MediaStore.Images.Media._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.DATA,
    )

    /**
     * Create the cursor to use for querying the media store
     *
     * @param uri the uri to query
     * @param selectionQuery a String representation to the conditions applied to the query
     * @param isVideo true if the query relates to the video media store
     * @return a [Cursor] of the query result
     */
    private fun createMediaCursor(
        uri: Uri,
        selectionQuery: String?,
        isVideo: Boolean
    ): Cursor? {
        val projection = getProjection(isVideo)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val args = Bundle().apply {
                val sortOrder = MediaStore.MediaColumns.DATE_MODIFIED + " ASC "
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                putString(ContentResolver.QUERY_ARG_OFFSET, "0")
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selectionQuery)
                debugSizeLimitation?.let {
                    putString(ContentResolver.QUERY_ARG_SQL_LIMIT, it.toString())
                }
            }
            context.contentResolver?.query(uri, projection, args, null)
        } else {
            val sortOrder = debugSizeLimitation?.let { "$this LIMIT 0,$it" }
            context.contentResolver?.query(uri, projection, selectionQuery, null, sortOrder)
        }
    }

    /**
     * Extract the media list using the cursor
     *
     * @param isVideo true if the type of media retrieved is
     * @return a Queue of [CameraUploadsMedia]
     */
    private fun Cursor.extractMedia(isVideo: Boolean): List<CameraUploadsMedia> =
        ArrayList<CameraUploadsMedia>().apply {
            if (moveToFirst()) {
                do {
                    val mediaId =
                        getLongValue(if (isVideo) MediaStore.Video.Media._ID else MediaStore.Images.Media._ID)
                    val displayName = getStringValue(MediaStore.MediaColumns.DISPLAY_NAME)
                    val filePath = getStringValue(MediaStore.MediaColumns.DATA)
                    val addedDate = getLongValue(MediaStore.MediaColumns.DATE_ADDED) * 1000
                    val modifiedDate = getLongValue(MediaStore.MediaColumns.DATE_MODIFIED) * 1000
                    val timestamp = max(addedDate, modifiedDate)

                    val cameraUploadsMedia = CameraUploadsMedia(
                        mediaId = mediaId,
                        displayName = displayName,
                        filePath = filePath,
                        timestamp = timestamp
                    )
                    add(cameraUploadsMedia)
                } while (moveToNext())
            }
            close()
        }.toList()

    /**
     *  Get the string value of a cursor column index
     */
    private fun Cursor.getStringValue(key: String) = getString(getColumnIndexOrThrow(key))

    /**
     *  Get the long value of a cursor column index
     */
    private fun Cursor.getLongValue(key: String) = getLong(getColumnIndexOrThrow(key))


    /**
     * Check if the uri corresponds to an uri of type video or not
     *
     * @return true if the uri corresponds to an uri of type video, false otherwise
     */
    private fun Uri.isVideoUri() = when (this) {
        MediaStore.Images.Media.INTERNAL_CONTENT_URI,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        -> false

        MediaStore.Video.Media.INTERNAL_CONTENT_URI,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        -> true

        else -> false
    }
}

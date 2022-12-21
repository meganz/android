package mega.privacy.android.data.facade

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.CameraUploadMediaGateway
import mega.privacy.android.domain.entity.CameraUploadMedia
import timber.log.Timber
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject
import kotlin.math.max

/**
 * Camera Upload Media Facade implements [CameraUploadMediaGateway]
 */
internal class CameraUploadMediaFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : CameraUploadMediaGateway {

    override suspend fun getMediaQueue(
        uri: Uri,
        parentPath: String?,
        isVideo: Boolean,
        selectionQuery: String?,
    ): Queue<CameraUploadMedia> =
        createMediaCursor(parentPath, selectionQuery, getPageSize(isVideo), uri)?.let {
            Timber.d("Extract ${it.count} Media from Cursor")
            extractMedia(it, parentPath)
        } ?: LinkedList<CameraUploadMedia>().also {
            Timber.d("Extract 0 Media - Cursor is NULL")
        }

    private fun getPageSize(isVideo: Boolean): Int = if (isVideo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) 50 else 10
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) 1000 else 400
    }

    private fun createMediaCursor(
        parentPath: String?,
        selectionQuery: String?,
        pageSize: Int,
        uri: Uri,
    ): Cursor? {
        val projection = getProjection()
        val mediaOrder = MediaStore.MediaColumns.DATE_MODIFIED + " ASC "
        return if (shouldPageCursor(parentPath)) {
            mediaOrder.getPagedMediaCursor(selectionQuery, pageSize, uri, projection)
        } else {
            context.contentResolver?.query(
                uri,
                projection,
                selectionQuery,
                null,
                mediaOrder
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun getProjection() = arrayOf(
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.DATE_MODIFIED
    )

    /**
     *  Only paging for files in internal storage
     *  Files on SD card usually have the same timestamp (the time when the SD is loaded)
     */
    private fun shouldPageCursor(parentPath: String?) =
        !isLocalFolderOnSDCard(context, parentPath)

    private fun String.getPagedMediaCursor(
        selectionQuery: String?,
        pageSize: Int,
        uri: Uri,
        projection: Array<String>,
    ): Cursor? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val args = Bundle()
            args.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, this)
            args.putString(ContentResolver.QUERY_ARG_OFFSET, "0")
            args.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selectionQuery)
            args.putString(ContentResolver.QUERY_ARG_SQL_LIMIT, pageSize.toString())
            context.contentResolver?.query(uri, projection, args, null)
        } else {
            val mediaOrderPreR = "$this LIMIT 0,$pageSize"
            context.contentResolver?.query(
                uri,
                projection,
                selectionQuery,
                null,
                mediaOrderPreR
            )
        }
    }

    private fun extractMedia(cursor: Cursor, parentPath: String?): Queue<CameraUploadMedia> {
        return LinkedList<CameraUploadMedia>().apply {
            try {
                @Suppress("DEPRECATION")
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val addedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val modifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    getUploadMediaFromCursor(
                        cursor,
                        dataColumn,
                        addedColumn,
                        modifiedColumn
                    ).takeIf {
                        isFilePathValid(it, parentPath)
                    }?.let(this::add)
                }
            } catch (exception: Exception) {
                Timber.e(exception)
            } finally {
                cursor.close()
            }
        }
    }

    private fun getUploadMediaFromCursor(
        cursor: Cursor,
        dataColumn: Int,
        addedColumn: Int,
        modifiedColumn: Int,
    ): CameraUploadMedia {
        val filePath = cursor.getString(dataColumn)
        val addedTime = cursor.getLong(addedColumn) * 1000
        val modifiedTime = cursor.getLong(modifiedColumn) * 1000
        val timestamp = max(addedTime, modifiedTime)
        val media = CameraUploadMedia(filePath = filePath, timestamp = timestamp)
        Timber.d("Extract from cursor, add time: $addedTime, modify time: $modifiedTime, chosen time: $timestamp")
        return media
    }

    private fun isFilePathValid(media: CameraUploadMedia, parentPath: String?) =
        media.filePath != null && !parentPath.isNullOrBlank()
                && media.filePath!!.startsWith(parentPath)


    // TODO Refactor SD Card Util Class
    private fun getSDCardRoot(path: String): String {
        var i = 0
        var x = 0
        val chars = path.toCharArray()
        while (x < chars.size) {
            val c = chars[x]
            if (c == '/') {
                i++
            }
            if (i == 3) {
                break
            }
            x++
        }
        return path.substring(0, x)
    }

    private fun isLocalFolderOnSDCard(context: Context, localPath: String?): Boolean {
        val fs = context.getExternalFilesDirs(null)
        if (fs.size > 1 && fs[1] != null) {
            val sdRoot = getSDCardRoot(fs[1].absolutePath)
            return localPath?.startsWith(sdRoot) ?: false
        }
        return false
    }
}

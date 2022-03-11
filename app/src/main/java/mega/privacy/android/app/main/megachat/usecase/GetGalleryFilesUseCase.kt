package mega.privacy.android.app.main.megachat.usecase

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.facebook.common.util.UriUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.utils.LogUtil
import javax.inject.Inject

/**
 * Use Coroutines To Load Images
 */
class GetGalleryFilesUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun get(): Flowable<List<FileGalleryItem>> =
        Flowable.create({ emitter ->
            val files = mutableListOf<FileGalleryItem>().apply {
                getFiles()
            }

            val requestFiles = files
                .sortedByDescending { it.dateAdded }
                .toMutableList()

            emitter.onNext(requestFiles!!)

            emitter.setCancellable {

            }
        }, BackpressureStrategy.LATEST)

    /**
     * Getting All Images Path.
     *
     * Required Storage Permission
     *
     * @return ArrayList with images Path
     */
    private fun getFiles(): ArrayList<FileGalleryItem>? {
        val files: ArrayList<FileGalleryItem> = ArrayList()

        val queryUri: Uri = MediaStore.Files.getContentUri("internal")

        val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.TITLE
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        context.contentResolver.query(
            queryUri,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mediaTypeColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mediaType = cursor.getInt(mediaTypeColumn)
                val date = cursor.getString(dateColumn)
                val contentUri = ContentUris.withAppendedId(
                    queryUri,
                    id
                )

                val path = "file://" + UriUtil.getRealPathFromUri(
                    context.contentResolver,
                    contentUri
                )

                val file = FileGalleryItem(
                    id,
                    mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                    path,
                    date,
                    null
                )
                files.add(file)
            }
        } ?: kotlin.run {
            LogUtil.logError("Cursor is null!")
            return null
        }
        return files
    }
}

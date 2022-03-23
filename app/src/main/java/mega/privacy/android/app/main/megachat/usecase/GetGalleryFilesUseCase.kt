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
import mega.privacy.android.app.utils.LogUtil.logError
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

            emitter.onNext(requestFiles)

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
        val queryUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.TITLE
        )

        context.contentResolver.query(
            queryUri,
            projection,
            "",
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val date = cursor.getString(dateColumn)
                val title = cursor.getString(titleColumn)
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
                    title,
                    path,
                    date,
                    null
                )
                files.add(file)
            }

        } ?: kotlin.run {
            logError("Cursor is null")
            return null
        }

        return files
    }
}

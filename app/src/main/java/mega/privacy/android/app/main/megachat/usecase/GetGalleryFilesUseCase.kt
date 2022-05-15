package mega.privacy.android.app.main.megachat.usecase

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.net.Uri
import android.provider.MediaStore
import com.facebook.common.util.UriUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.TimeUtils
import javax.inject.Inject

/**
 * Use case to retrieve images and videos from the gallery.
 */
class GetGalleryFilesUseCase @Inject constructor(
        @ApplicationContext private val context: Context
) {

    fun get(): Flowable<List<FileGalleryItem>> =
            Flowable.create({ emitter ->
                val files = mutableListOf<FileGalleryItem>().apply {
                    addAll(fetchImages())
                    addAll(fetchVideos())
                }

                val requestFiles = files
                        .sortedByDescending { it.dateAdded }
                        .toMutableList()

                emitter.onNext(requestFiles)

            }, BackpressureStrategy.LATEST)

    /**
     * Method to get the images from the gallery
     *
     * @return ArrayList<FileGalleryItem> List of images
     */
    fun fetchImages(): ArrayList<FileGalleryItem> {
        val imageList: ArrayList<FileGalleryItem> = ArrayList()

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
                val path = UriUtil.getRealPathFromUri(
                        context.contentResolver,
                        contentUri
                )

                val file = FileGalleryItem(
                        id = id,
                        isImage = true,
                        title = title,
                        fileUri = contentUri,
                        dateAdded = date,
                        duration = "",
                        isSelected = false,
                        filePath = path
                )

                imageList.add(file)
            }

        } ?: kotlin.run {
            logError("Cursor is null")
        }

        return imageList
    }

    /**
     * Method to get the videos from the gallery
     *
     * @return ArrayList<FileGalleryItem> List of videos
     */
    fun fetchVideos(): ArrayList<FileGalleryItem> {
        val videoList: ArrayList<FileGalleryItem> = ArrayList()

        val queryUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.TITLE
        )

        context.contentResolver.query(
                queryUri,
                projection,
                "",
                null,
                sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val date = cursor.getString(dateColumn)
                val title = cursor.getString(titleColumn)

                val contentUri = ContentUris.withAppendedId(
                        queryUri,
                        id
                )

                val path = UriUtil.getRealPathFromUri(
                        context.contentResolver,
                        contentUri
                )

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, contentUri)
                val duration = retriever.extractMetadata(METADATA_KEY_DURATION)
                retriever.release()
                val durationText = TimeUtils.getGalleryVideoDuration(duration?.toLongOrNull() ?: 0)

                val file = FileGalleryItem(
                        id = id,
                        isImage = false,
                        title = title,
                        fileUri = contentUri,
                        dateAdded = date,
                        duration = durationText,
                        isSelected = false,
                        filePath = path
                )
                videoList.add(file)
            }

        } ?: kotlin.run {
            logError("Cursor is null")
        }

        return videoList
    }
}

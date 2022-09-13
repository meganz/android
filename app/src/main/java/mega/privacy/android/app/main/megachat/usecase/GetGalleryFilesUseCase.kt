package mega.privacy.android.app.main.megachat.usecase

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.utils.TimeUtils
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to retrieve images and videos from the gallery.
 */
class GetGalleryFilesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        const val DATA = "_data"
    }

    /**
     * Get list of images and videos
     *
     * @return MutableList<FileGalleryItem>
     */
    fun getFiles(): Flowable<List<FileGalleryItem>> =
        Flowable.create({ emitter ->
            val files = mutableListOf<FileGalleryItem>()

            fetchVideos()
                .subscribeOn(Schedulers.newThread())
                .subscribeBy(
                    onNext = { item ->
                        files.add(item)
                        emitter.onNext(files.sortedByDescending { it.dateAdded })
                    },
                    onError = Timber::e,
                    onComplete = {
                        Timber.d("All videos have been loaded")
                    }
                )

            fetchImages()
                .subscribeOn(Schedulers.newThread())
                .subscribeBy(
                    onNext = { item ->
                        files.add(item)
                        emitter.onNext(files.sortedByDescending { it.dateAdded })
                    },
                    onError = Timber::e,
                    onComplete = {
                        Timber.d("All images have been loaded")
                    }
                )
        }, BackpressureStrategy.LATEST)

    /**
     * Method to get the images from the gallery
     *
     * @return ArrayList<FileGalleryItem> List of images
     */
    fun fetchImages(): Flowable<FileGalleryItem> =
        Flowable.create({ emitter ->
            val queryUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.TITLE,
                DATA
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
                val dataColumn = cursor.getColumnIndexOrThrow(DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val date = cursor.getString(dateColumn)
                    val title = cursor.getString(titleColumn)
                    val contentUri = ContentUris.withAppendedId(
                        queryUri,
                        id
                    )
                    val path = cursor.getString(dataColumn)

                    val file = FileGalleryItem(
                        id = id,
                        isImage = true,
                        isTakePicture = false,
                        title = title,
                        fileUri = contentUri,
                        dateAdded = date,
                        duration = "",
                        isSelected = false,
                        filePath = path
                    )

                    emitter.onNext(file)
                }

            } ?: kotlin.run {
                Timber.e("Cursor is null")
            }

            emitter.onComplete()

        }, BackpressureStrategy.BUFFER)

    /**
     * Method to get the videos from the gallery
     *
     * @return ArrayList<FileGalleryItem> List of videos
     */
    fun fetchVideos(): Flowable<FileGalleryItem> =
        Flowable.create({ emitter ->
            val queryUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.TITLE,
                DATA
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
                val dataColumn = cursor.getColumnIndexOrThrow(DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val date = cursor.getString(dateColumn)
                    val title = cursor.getString(titleColumn)

                    val contentUri = ContentUris.withAppendedId(
                        queryUri,
                        id
                    )

                    val path = cursor.getString(dataColumn)
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, contentUri)
                    val duration = retriever.extractMetadata(METADATA_KEY_DURATION)
                    retriever.release()
                    val durationText =
                        TimeUtils.getGalleryVideoDuration(duration?.toLongOrNull() ?: 0)
                    val file = FileGalleryItem(
                        id = id,
                        isImage = false,
                        isTakePicture = false,
                        title = title,
                        fileUri = contentUri,
                        dateAdded = date,
                        duration = durationText,
                        isSelected = false,
                        filePath = path
                    )
                    emitter.onNext(file)
                }

            } ?: kotlin.run {
                Timber.e("Cursor is null")
            }
            emitter.onComplete()

        }, BackpressureStrategy.BUFFER)
}

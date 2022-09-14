package mega.privacy.android.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.chat.FileGalleryItem
import mega.privacy.android.domain.repository.GalleryFilesRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * The repository implementation class regarding gallery files
 * @param ioDispatcher IODispatcher
 */
class DefaultGalleryFilesRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
) : GalleryFilesRepository {

    companion object {
        /**
         * To get data of file
         */
        const val DATA = "_data"
    }

    override fun getAllGalleryFiles(): Flow<List<FileGalleryItem>> {
        return callbackFlow {
            val files = mutableListOf<FileGalleryItem>()

            fetchVideos()
                .filter { file ->
                    !files.contains(file)
                }.map { file ->
                    Timber.d("************ Recuperado 1 video")
                    files.add(file)
                    trySend(files.sortedByDescending { it.dateAdded })
                }

            fetchImages()
                .filter { file ->
                    !files.contains(file)
                }.map { file ->
                    Timber.d("************ Recuperado 1 imagen")
                    files.add(file)
                    trySend(files.sortedByDescending { it.dateAdded })
                }
        }
    }

    /**
     * Method to get the images from the gallery
     *
     * @return ArrayList<FileGalleryItem> List of images
     */
    fun fetchImages(): Flow<FileGalleryItem> {
        return callbackFlow {
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
                        fileUri = contentUri.toString(),
                        dateAdded = date,
                        duration = "",
                        isSelected = false,
                        filePath = path
                    )

                    trySend(file)
                }

            } ?: kotlin.run {
                Timber.e("Cursor is null")
            }

            awaitClose { cancel() }
        }
    }

    /**
     * Method to get the videos from the gallery
     *
     * @return ArrayList<FileGalleryItem> List of videos
     */
    fun fetchVideos(): Flow<FileGalleryItem> {
        return callbackFlow {
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
                    val duration =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    //retriever.release()
                    val durationText =
                        TimeUtils.getGalleryVideoDuration(duration?.toLongOrNull() ?: 0)
                    val file = FileGalleryItem(
                        id = id,
                        isImage = false,
                        isTakePicture = false,
                        title = title,
                        fileUri = contentUri.toString(),
                        dateAdded = date,
                        duration = durationText,
                        isSelected = false,
                        filePath = path
                    )
                    trySend(file)
                }

            } ?: kotlin.run {
                Timber.e("Cursor is null")
            }

        }
    }
}
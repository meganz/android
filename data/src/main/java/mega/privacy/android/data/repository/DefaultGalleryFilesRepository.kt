package mega.privacy.android.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.domain.entity.chat.FileGalleryItem
import mega.privacy.android.domain.repository.GalleryFilesRepository
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The repository implementation class regarding gallery files
 */
internal class DefaultGalleryFilesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : GalleryFilesRepository {

    companion object {
        /**
         * To get data of file
         */
        const val DATA = "_data"
    }

    /**
     * Method to get the images from the gallery
     *
     * @return  Flow of List of [FileGalleryItem].
     */
    override fun getAllGalleryImages(): Flow<FileGalleryItem> =
        callbackFlow {
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
                        dateAdded = date.toLong(),
                        duration = "",
                        isSelected = false,
                        filePath = path
                    )

                    trySend(file)
                }

            } ?: kotlin.run {
                Timber.e("Cursor is null")
            }

            awaitClose {}
        }

    /**
     * Method to get the images from the gallery
     *
     * @return  Flow of List of [FileGalleryItem].
     */
    override fun getAllGalleryVideos(): Flow<FileGalleryItem> =
        callbackFlow {
            val queryUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.TITLE,
                DATA
            )

            val retriever = MediaMetadataRetriever()

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

                    try {
                        retriever.setDataSource(context, contentUri)
                        val duration =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                        var durationText = ""
                        if (duration != null) {
                            durationText = getGalleryVideoDuration(duration.toLong())
                        } else {
                            Timber.w("No duration found")
                        }

                        val file = FileGalleryItem(
                            id = id,
                            isImage = false,
                            isTakePicture = false,
                            title = title,
                            fileUri = contentUri.toString(),
                            dateAdded = date.toLong(),
                            duration = durationText,
                            isSelected = false,
                            filePath = path
                        )

                        trySend(file)

                    } catch (ex: Exception) {
                        Timber.e(ex)
                    }
                }
            } ?: kotlin.run {
                Timber.e("Cursor is null")
            }
            awaitClose {
                retriever.release()
            }
        }

    /**
     * Method of getting the appropriate string from a given duration
     *
     * @param duration The duration
     * @return The appropriate string
     */
    private fun getGalleryVideoDuration(duration: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(duration)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toSeconds(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
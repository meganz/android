package mega.privacy.android.app.lollipop.tasks

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.facebook.common.util.UriUtil
import mega.privacy.android.app.lollipop.megachat.ChatActivity
import mega.privacy.android.app.lollipop.megachat.FileGalleryItem
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import java.util.*

class FetchDeviceGalleryTask(var context: Context?) :
    AsyncTask<Void?, Void?, List<FileGalleryItem>?>() {

    override fun onPostExecute(photoUris: List<FileGalleryItem>?) {
        (context as ChatActivity).showGallery(photoUris as ArrayList<FileGalleryItem>?)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun doInBackground(vararg p0: Void?): List<FileGalleryItem>? {
        if (context != null) {
            val files: MutableList<FileGalleryItem> = mutableListOf()

            val queryUri: Uri = MediaStore.Files.getContentUri("external")

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
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.DURATION
            )

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

            val cursor = context!!.contentResolver.query(
                queryUri,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor.use {
                it?.let {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val mediaTypeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val dateColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                    val durationColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)

                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val mediaType = it.getInt(mediaTypeColumn)
                        val date = it.getString(dateColumn)
                        val contentUri = ContentUris.withAppendedId(
                            queryUri,
                            id
                        )
                        val path = "file://" + UriUtil.getRealPathFromUri(
                            context!!.contentResolver,
                            contentUri
                        )
                        val file: FileGalleryItem =
                            if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                                FileGalleryItem(id, true, 0, path, date)
                            } else {
                                val duration = it.getLong(durationColumn)
                                FileGalleryItem(id, false, duration, path, date)
                            }

                        files.add(file)
                    }
                } ?: kotlin.run {
                    logError("Cursor is null!")
                    return null
                }
            }

            return files
        }

        return null
    }
}
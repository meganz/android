package mega.privacy.android.app.lollipop.tasks

import android.content.ContentUris
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.facebook.common.util.UriUtil
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop
import mega.privacy.android.app.lollipop.megachat.FileGalleryItem
import mega.privacy.android.app.utils.LogUtil.logError

class FetchDeviceGalleryTask(var context: Context?) :
    AsyncTask<Void?, Void?, List<FileGalleryItem>?>() {

    override fun onPostExecute(photoUris: List<FileGalleryItem>?) {
        (context as ChatActivityLollipop).showGallery(photoUris as ArrayList<FileGalleryItem>?)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun doInBackground(vararg p0: Void?): List<FileGalleryItem>? {
        if (context != null) {
            val files: MutableList<FileGalleryItem> = mutableListOf()

            val uriImagesExternal = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uriVideosExternal = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            val imageProjection = arrayOf(
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media._ID
            )

            val videoProjection = arrayOf(
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DURATION
            )

            val imageSortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val videoSortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            val imagesCursor = context!!.contentResolver.query(
                uriImagesExternal,
                imageProjection,
                null,
                null,
                imageSortOrder
            )

            val videosCursor = context!!.contentResolver.query(
                uriVideosExternal,
                videoProjection,
                null,
                null,
                videoSortOrder
            )

            videosCursor.use {
                it?.let {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val dateColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val date = it.getString(dateColumn)
                        val duration = it.getString(durationColumn)

                        val contentUri = ContentUris.withAppendedId(
                            uriVideosExternal,
                            id
                        )

                        val path = "file://" + UriUtil.getRealPathFromUri(
                            context!!.contentResolver,
                            contentUri
                        )
                        val file = FileGalleryItem(false, duration, path, date)
                        files.add(file)
                    }
                } ?: kotlin.run {
                    logError("Cursor is null!")
                    return null
                }
            }

            imagesCursor.use {
                it?.let {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val date = it.getString(dateColumn)
                        val contentUri = ContentUris.withAppendedId(
                            uriImagesExternal,
                            id
                        )

                        val path = "file://" + UriUtil.getRealPathFromUri(
                            context!!.contentResolver,
                            contentUri
                        )
                        val file = FileGalleryItem(true, null, path, date)
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
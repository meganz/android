package mega.privacy.android.app.lollipop.tasks

import android.content.ContentUris
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.RequiresApi
import com.facebook.common.util.UriUtil
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop
import mega.privacy.android.app.lollipop.megachat.FileGalleryItem
import mega.privacy.android.app.utils.LogUtil.logError
import kotlin.collections.ArrayList

class FetchDeviceGalleryTask(var context: Context?) :
    AsyncTask<Void?, Void?, List<FileGalleryItem>?>() {

    override fun onPostExecute(photoUris: List<FileGalleryItem>?) {
        (context as ChatActivityLollipop).showGallery(photoUris as ArrayList<FileGalleryItem>?)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun doInBackground(vararg p0: Void?): List<FileGalleryItem>? {
        if (context != null) {
            val uriImagesExternal = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val imageProjection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media._ID
            )

            val imageSortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

            val imagesCursor = context!!.contentResolver.query(
                uriImagesExternal,
                imageProjection,
                null,
                null,
                imageSortOrder
            )

            imagesCursor.use {
                it?.let {
                    val files: MutableList<FileGalleryItem> = mutableListOf()

                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val name = it.getString(nameColumn)
                        val size = it.getString(sizeColumn)
                        val date = it.getString(dateColumn)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        val path = "file://" + UriUtil.getRealPathFromUri(context!!.contentResolver, contentUri)

                        val file = FileGalleryItem(true, 0, path)
                        files.add(file)
                    }

                    return files
                } ?: kotlin.run {
                    logError("Cursor is null!")
                }
            }
        }
        return null
    }
}
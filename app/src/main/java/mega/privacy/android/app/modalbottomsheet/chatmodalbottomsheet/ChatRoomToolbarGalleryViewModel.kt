package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns.DATA
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.LogUtil.logDebug
import kotlin.coroutines.CoroutineContext

/**
 * Use Coroutines To Load Images
 */
class ChatRoomToolbarGalleryViewModel() : ViewModel(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // List of participants in the meeting
    val imagesLiveData: MutableLiveData<List<String>> = MutableLiveData()

    fun getImageList(): MutableLiveData<List<String>> {
        return imagesLiveData
    }

    /**
     * Getting All Images Path.
     *
     * Required Storage Permission
     *
     * @return ArrayList with images Path
     */
    internal fun loadImages(): ArrayList<String> {
        val listOfAllImages = ArrayList<String>()
        /*val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        var absolutePathOfImage: String? = null
        val projection =
            arrayOf(DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

        val cursor =
            MegaApplication.getInstance().contentResolver.query(uri, projection, null, null, null)

        val columnIndexData = cursor!!.getColumnIndexOrThrow(DATA)
        val columnIndexFolderName = cursor!!
            .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        while (cursor!!.moveToNext()) {
            absolutePathOfImage = cursor!!.getString(columnIndexData)
            listOfAllImages.add(absolutePathOfImage)
        }*/

        return listOfAllImages
    }

    fun getAllImages() {
        /*launch(Dispatchers.Main) {
            imagesLiveData.value = withContext(Dispatchers.IO) {
                loadImages()
            }
        }*/
    }
}
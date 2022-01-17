package mega.privacy.android.app.upload

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.upload.data.FolderItem

/**
 * ViewModel which manages data of [UploadFolderActivity]
 */
class UploadFolderViewModel : BaseRxViewModel() {

    private lateinit var parentDocument: DocumentFile
    private val currentFolder: MutableLiveData<String> = MutableLiveData()
    val items: MutableLiveData<List<FolderItem>> = MutableLiveData()

    fun getCurrentFolder(): LiveData<String> = currentFolder
    fun getFolderContent(): LiveData<List<FolderItem>> = items

    fun retrieveFolderContent(documentFile: DocumentFile) {
        parentDocument = documentFile
        currentFolder.value = documentFile.name
    }
}
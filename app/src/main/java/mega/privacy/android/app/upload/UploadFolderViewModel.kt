package mega.privacy.android.app.upload

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.arch.BaseRxViewModel

/**
 * ViewModel which manages data of [UploadFolderActivity]
 */
class UploadFolderViewModel : BaseRxViewModel() {

    private lateinit var parentDocument: DocumentFile
    private val currentFolder: MutableLiveData<String> = MutableLiveData()
    private val folderItems: MutableLiveData<List<DocumentFile>> = MutableLiveData()

    fun getCurrentFolder(): LiveData<String> = currentFolder
    fun getFolderContent(): LiveData<List<DocumentFile>> = folderItems

    fun retrieveFolderContent(documentFile: DocumentFile) {
        parentDocument = documentFile
        currentFolder.value = documentFile.name
        folderItems.value = documentFile.listFiles().toList()
    }
}
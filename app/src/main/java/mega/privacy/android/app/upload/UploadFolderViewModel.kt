package mega.privacy.android.app.upload

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.upload.list.data.FolderContent

/**
 * ViewModel which manages data of [UploadFolderActivity]
 */
class UploadFolderViewModel : BaseRxViewModel() {

    private val currentFolder: MutableLiveData<FolderContent.Data> = MutableLiveData()
    private val folderItems: MutableLiveData<List<FolderContent>> = MutableLiveData()

    fun getCurrentFolder(): LiveData<FolderContent.Data> = currentFolder
    fun getFolderContent(): LiveData<List<FolderContent>> = folderItems

    fun retrieveFolderContent(documentFile: DocumentFile) {
        currentFolder.value = FolderContent.Data(null, documentFile)
        setFolderItems()
    }

    private fun setFolderItems() {
        val listFiles = currentFolder.value?.document?.listFiles()
        folderItems.value = if (listFiles.isNullOrEmpty()) {
            emptyList()
        } else {
            val folderContentList = ArrayList<FolderContent>()
            folderContentList.add(FolderContent.Header())

            listFiles.forEach { file ->
                folderContentList.add(FolderContent.Data(currentFolder.value, file))
            }

            folderContentList
        }
    }

    fun folderClick(folderClicked: FolderContent.Data) {
        currentFolder.value = folderClicked
        setFolderItems()
    }

    fun back(): Boolean =
        if (currentFolder.value?.parent == null) {
            true
        } else {
            currentFolder.value = currentFolder.value?.parent
            setFolderItems()
            false
        }
}
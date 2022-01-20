package mega.privacy.android.app.upload

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.upload.list.data.FolderContent
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * ViewModel which manages data of [UploadFolderActivity]
 */
@HiltViewModel
class UploadFolderViewModel @Inject constructor(
    private val getFolderContentUseCase: GetFolderContentUseCase
) : BaseRxViewModel() {

    private val currentFolder: MutableLiveData<FolderContent.Data> = MutableLiveData()
    private val folderItems: MutableLiveData<List<FolderContent>> = MutableLiveData()

    private var order: Int = MegaApiJava.ORDER_DEFAULT_ASC
    private val folderContent = HashMap<FolderContent.Data?, List<FolderContent>>()

    fun getCurrentFolder(): LiveData<FolderContent.Data> = currentFolder
    fun getFolderContent(): LiveData<List<FolderContent>> = folderItems

    fun retrieveFolderContent(documentFile: DocumentFile, order: Int) {
        currentFolder.value = FolderContent.Data(null, documentFile)
        this.order = order
        setFolderItems()
    }

    private fun setFolderItems() {
        val items = folderContent[currentFolder.value]
        if (items != null) {
            folderItems.value = items
            return
        }

        getFolderContentUseCase.get(currentFolder.value!!, order)
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { folderItems.value = emptyList() },
                onSuccess = { items ->
                    folderItems.value = items
                    folderContent[currentFolder.value] = items
                }
            )
            .addTo(composite)
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

    fun setOrder(newOrder: Int) {
        if (newOrder != order) {
            order = newOrder

            if (!folderItems.value.isNullOrEmpty()) {
                getFolderContentUseCase.reorder(folderItems.value!!, order)
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onError = { folderItems.value = emptyList() },
                        onSuccess = { items -> folderItems.value = items }
                    )
                    .addTo(composite)
            }
        }
    }
}
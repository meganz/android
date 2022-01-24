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
import mega.privacy.android.app.components.textFormatter.TextFormatterUtils.INVALID_INDEX
import mega.privacy.android.app.upload.list.data.FolderContent
import mega.privacy.android.app.upload.usecase.GetFolderContentUseCase
import mega.privacy.android.app.utils.notifyObserver
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
    private val folderItems: MutableLiveData<MutableList<FolderContent>> = MutableLiveData()
    private val selectedItems: MutableLiveData<MutableList<FolderContent.Data>> = MutableLiveData()

    private var order: Int = MegaApiJava.ORDER_DEFAULT_ASC
    private val folderContent = HashMap<FolderContent.Data?, MutableList<FolderContent>>()
    private var query: String? = null

    fun getCurrentFolder(): LiveData<FolderContent.Data> = currentFolder
    fun getFolderItems(): LiveData<MutableList<FolderContent>> = folderItems
    fun getSelectedItems(): LiveData<MutableList<FolderContent.Data>> = selectedItems

    fun retrieveFolderContent(documentFile: DocumentFile, order: Int) {
        currentFolder.value = FolderContent.Data(null, documentFile)
        selectedItems.value = mutableListOf()
        this.order = order
        setFolderItems()
    }

    fun getFolderContentItems(): MutableList<FolderContent>? =
        folderItems.value

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
                onError = { folderItems.value = mutableListOf() },
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
                        onError = { folderItems.value = mutableListOf() },
                        onSuccess = { items -> folderItems.value = items }
                    )
                    .addTo(composite)
            }
        }
    }

    fun itemLongClick(itemClicked: FolderContent.Data) {
        folderItems.value?.apply {
            val index = lastIndexOf(itemClicked)

            if (index != INVALID_INDEX) {
                itemClicked.longClick()
                set(index, itemClicked)

                selectedItems.value?.apply {
                    if (itemClicked.isSelected) {
                        add(itemClicked)
                    } else {
                        remove(itemClicked)
                    }

                    selectedItems.notifyObserver()
                }
            }
        }
    }

    fun clearSelected(): List<Int> {
        selectedItems.value = mutableListOf()

        val positions = mutableListOf<Int>()

        folderItems.value?.apply {
            for (item in this) {
                if (item is FolderContent.Data && item.isSelected) {
                    positions.add(indexOf(item))
                    item.longClick()
                }
            }
        }

        return positions
    }

    fun search(newQuery: String?) {
        query = newQuery
    }
}
package mega.privacy.android.app.upload

import android.net.Uri
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
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.notifyObserver
import nz.mega.sdk.MegaApiJava
import java.util.ArrayList
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
    private val selectedItems: MutableLiveData<MutableList<Int>> = MutableLiveData()

    private var order: Int = MegaApiJava.ORDER_DEFAULT_ASC
    private var isList: Boolean = true
    private var query: String? = null
    private var folderContent = HashMap<FolderContent.Data?, MutableList<FolderContent>>()
    private var searchResults =
        HashMap<FolderContent.Data, HashMap<String, MutableList<FolderContent>>>()

    fun getCurrentFolder(): LiveData<FolderContent.Data> = currentFolder
    fun getFolderItems(): LiveData<MutableList<FolderContent>> = folderItems
    fun getSelectedItems(): LiveData<MutableList<Int>> = selectedItems

    fun retrieveFolderContent(documentFile: DocumentFile, order: Int, isList: Boolean) {
        currentFolder.value = FolderContent.Data(null, documentFile)
        selectedItems.value = mutableListOf()
        this.order = order
        this.isList = isList
        setFolderItems()
    }

    fun getQuery(): String? = query

    fun isSearchInProgress(): Boolean =
        !query.isNullOrEmpty() && !isSearchAlreadyDone()

    private fun isSearchAlreadyDone(): Boolean =
        searchResults.containsKey(currentFolder.value)
                && searchResults[currentFolder.value]!!.containsKey(query)

    private fun setFolderItems() {
        val items = folderContent[currentFolder.value]
        if (items != null) {
            folderItems.value = items
            return
        }

        getFolderContentUseCase.get(currentFolder.value!!, order, isList)
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { folderItems.value = mutableListOf() },
                onSuccess = { finalItems ->
                    folderItems.value = finalItems
                    folderContent[currentFolder.value] = finalItems
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
                getFolderContentUseCase.reorder(folderItems.value!!, order, isList)
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onError = { folderItems.value = mutableListOf() },
                        onSuccess = { items ->
                            folderItems.value = items

                            if (query == null && folderContent.containsKey(currentFolder.value!!)) {
                                folderContent[currentFolder.value] = items
                            }
                        }
                    )
                    .addTo(composite)
            }

            val folder: FolderContent.Data? = if (query == null) currentFolder.value else null

            getFolderContentUseCase.reorder(folderContent, folder, order, isList)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { error -> logWarning("Cannot reorder", error) },
                    onSuccess = { newFolderContent -> folderContent = newFolderContent }
                )
                .addTo(composite)
        }
    }

    fun setIsList(newIsList: Boolean) {
        if (newIsList != isList) {
            isList = newIsList

            if (!folderItems.value.isNullOrEmpty()) {
                getFolderContentUseCase.switchView(folderItems.value!!, isList)
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onError = { error -> logWarning("No switch view.", error) },
                        onSuccess = { items ->
                            folderItems.value = items

                            if (query == null && folderContent.containsKey(currentFolder.value!!)) {
                                folderContent[currentFolder.value] = items
                            }
                        }
                    )
                    .addTo(composite)
            }

            val folder: FolderContent.Data? = if (query == null) currentFolder.value else null

            getFolderContentUseCase.switchView(folderContent, folder, isList)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { error -> logWarning("Cannot switch view.", error) },
                    onSuccess = { newFolderContent -> folderContent = newFolderContent }
                )
                .addTo(composite)
        }
    }

    fun itemLongClick(itemClicked: FolderContent.Data) {
        val index = folderItems.value?.lastIndexOf(itemClicked) ?: INVALID_INDEX
        if (index == INVALID_INDEX) {
            return
        }

        selectedItems.value?.apply {
            if (itemClicked.isSelected) {
                remove(index)
            } else {
                add(index)
            }

            selectedItems.notifyObserver()
        }
    }

    fun finishSelection() {
        val finalList = mutableListOf<FolderContent>()

        folderItems.value?.apply {
            for (item in this) {
                if (item is FolderContent.Data) {
                    val index = indexOf(item)
                    val selected = selectedItems.value?.contains(index) ?: false

                    if (item.isSelected == selected) {
                        finalList.add(item)
                    } else {
                        finalList.add(FolderContent.Data(item.parent, item.document, selected))
                    }
                } else {
                    finalList.add(item)
                }
            }
        }

        folderItems.value = finalList
    }

    fun clearSelected(): List<Int> {
        selectedItems.value = mutableListOf()

        val positions = mutableListOf<Int>()

        folderItems.value?.apply {
            for (item in this) {
                if (item is FolderContent.Data && item.isSelected) {
                    positions.add(indexOf(item))
                }
            }
        }

        return positions
    }

    fun search(newQuery: String?) {
        query = newQuery

        if (newQuery.isNullOrEmpty()) {
            setFolderItems()
        } else {
            if (isSearchAlreadyDone()) {
                folderItems.value = searchResults[currentFolder.value]!![newQuery]
                return
            }

            getFolderContentUseCase.search(newQuery, currentFolder.value!!, order, isList)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { addSearchValue(currentFolder.value!!, newQuery, mutableListOf()) },
                    onSuccess = { searchResult ->
                        addSearchValue(currentFolder.value!!, newQuery, searchResult)
                    }
                )
                .addTo(composite)
        }
    }

    private fun addSearchValue(
        folder: FolderContent.Data,
        query: String,
        searchResult: MutableList<FolderContent>
    ) {
        if (searchResults.containsKey(folder)) {
            searchResults.getValue(folder)[query] = searchResult
        } else {
            val map = hashMapOf<String, MutableList<FolderContent>>().apply {
                put(query, searchResult)
            }

            searchResults[folder] = map
        }

        folderItems.value = searchResult
    }

    fun upload(): ArrayList<Uri> {
        val uploadList = ArrayList<Uri>()

        if (selectedItems.value?.isEmpty() == false) {
            selectedItems.value?.forEach { item ->
                uploadList.add((folderItems.value?.get(item) as FolderContent.Data).uri)
            }
        } else {
            folderItems.value?.forEach { item ->
                if (item is FolderContent.Data) {
                    uploadList.add(item.uri)
                }
            }
        }

        return uploadList
    }
}
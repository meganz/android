package mega.privacy.android.app.uploadFolder

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.textFormatter.TextFormatterUtils.INVALID_INDEX
import mega.privacy.android.app.domain.exception.EmptyFolderException
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.uploadFolder.usecase.GetFolderContentUseCase
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.notifyObserver
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import javax.inject.Inject

/**
 * ViewModel which manages data of [UploadFolderActivity].
 *
 * @property getFolderContentUseCase    Required for getting folder content.
 * @property checkNameCollisionUseCase  Required for checking name collisions.
 * @property transfersManagement        Required for checking transfers status.
 */
@HiltViewModel
class UploadFolderViewModel @Inject constructor(
    private val getFolderContentUseCase: GetFolderContentUseCase,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val transfersManagement: TransfersManagement
) : BaseRxViewModel() {

    private val currentFolder: MutableLiveData<FolderContent.Data> = MutableLiveData()
    private val folderItems: MutableLiveData<MutableList<FolderContent>> = MutableLiveData()
    private val selectedItems: MutableLiveData<MutableList<Int>> = MutableLiveData()
    private val collisions: MutableLiveData<ArrayList<NameCollision>> = MutableLiveData()
    private val actionResult: MutableLiveData<String?> = MutableLiveData()

    private lateinit var parentFolder: String
    private var parentHandle: Long = INVALID_HANDLE
    private var order: Int = MegaApiJava.ORDER_DEFAULT_ASC
    private var isList: Boolean = true
    var query: String? = null
    private var isPendingToFinishSelection = false
    private var getContentDisposable: Disposable? = null
    private var nameCollisionDisposable: Disposable? = null
    private var folderContent = HashMap<FolderContent.Data?, MutableList<FolderContent>>()
    private var searchResults =
        HashMap<FolderContent.Data, HashMap<String, MutableList<FolderContent>>>()
    private var pendingUploads: MutableList<FolderContent.Data> = mutableListOf()

    fun getCurrentFolder(): LiveData<FolderContent.Data> = currentFolder
    fun getFolderItems(): LiveData<MutableList<FolderContent>> = folderItems
    fun getSelectedItems(): LiveData<MutableList<Int>> = selectedItems
    fun getCollisions(): LiveData<ArrayList<NameCollision>> = collisions
    fun onActionResult(): LiveData<String?> = actionResult

    /**
     * Initializes the view model with the initial data.
     *
     * @param documentFile  DocumentFile picked.
     * @param parentHandle  Handle of the parent node in which the content will be uploaded.
     * @param order         Current order.
     * @param isList        True if the view is list, false if it is grid.
     */
    fun retrieveFolderContent(
        documentFile: DocumentFile,
        parentHandle: Long,
        order: Int,
        isList: Boolean
    ) {
        parentFolder = documentFile.name.toString()
        currentFolder.value = FolderContent.Data(null, documentFile)
        selectedItems.value = mutableListOf()
        this.parentHandle = parentHandle
        this.order = order
        this.isList = isList
        setFolderItems()
    }

    /**
     * Checks if there is a search in progress.
     *
     * @return True if there is a search in progress, false otherwise.
     */
    fun isSearchInProgress(): Boolean =
        !query.isNullOrEmpty() && !isSearchAlreadyDone()


    /**
     * Checks if a search has been already done.
     *
     * @return True if the search has been already done, false otherwise.
     */
    private fun isSearchAlreadyDone(): Boolean =
        searchResults.containsKey(currentFolder.value)
                && searchResults[currentFolder.value]!!.containsKey(query)

    /**
     * Updates the current folder items with the current folder content.
     * If the content is already get, only sets it. If not, requests it.
     */
    private fun setFolderItems() {
        val items = folderContent[currentFolder.value]
        if (items != null) {
            folderItems.value = items
            return
        }

        getFolderContentUseCase.get(currentFolder.value!!, order, isList)
            .subscribeOn(Schedulers.io())
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

    /**
     * Performs a click in a folder item.
     *
     * @param folderClicked The clicked folder.
     */
    fun folderClick(folderClicked: FolderContent.Data) {
        currentFolder.value = folderClicked
        setFolderItems()
    }

    /**
     * Performs the back action.
     *
     * @return True if it is already in the parent folder, false otherwise.
     */
    fun back(): Boolean =
        if (currentFolder.value?.parent == null) {
            true
        } else {
            currentFolder.value = currentFolder.value?.parent
            setFolderItems()
            false
        }

    /**
     * Updates the order.
     *
     * @param newOrder The new order to set.
     */
    fun setOrder(newOrder: Int) {
        if (newOrder != order) {
            order = newOrder

            if (!folderItems.value.isNullOrEmpty()) {
                getFolderContentUseCase.reorder(folderItems.value!!, order, isList)
                    .subscribeOn(Schedulers.io())
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
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { error -> logWarning("Cannot reorder", error) },
                    onSuccess = { newFolderContent -> folderContent = newFolderContent }
                )
                .addTo(composite)
        }
    }

    /**
     * Updates the view type.
     *
     * @param newIsList True if the new view type is list, false if is grid.
     */
    fun setIsList(newIsList: Boolean) {
        if (newIsList != isList) {
            isList = newIsList

            if (!folderItems.value.isNullOrEmpty()) {
                getFolderContentUseCase.switchView(folderItems.value!!, isList)
                    .subscribeOn(Schedulers.io())
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
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { error -> logWarning("Cannot switch view.", error) },
                    onSuccess = { newFolderContent -> folderContent = newFolderContent }
                )
                .addTo(composite)
        }
    }

    /**
     * Performs a long click in an item and adds or removes an item to the selected list
     * depending on its previous state.
     *
     * @param itemClicked   The clicked item.
     */
    fun itemLongClick(itemClicked: FolderContent.Data) {
        val index = folderItems.value?.lastIndexOf(itemClicked) ?: INVALID_INDEX
        if (index == INVALID_INDEX) {
            return
        }

        selectedItems.value?.apply {
            when {
                !itemClicked.isSelected -> add(index)
                size == 1 -> isPendingToFinishSelection = true
                else -> remove(index)
            }
        }

        if (!isPendingToFinishSelection) {
            selectedItems.notifyObserver()
            finishSelection()
        }
    }

    /**
     * Finishes the select action, updating the selected items in the folder items.
     *
     * @param remove True if the selection has been removed, false otherwise.
     */
    private fun finishSelection(remove: Boolean = false) {
        val finalList = mutableListOf<FolderContent>()

        if (remove) {
            selectedItems.value?.clear()
        }

        folderItems.value?.apply {
            for (item in this) {
                val index = indexOf(item)
                val selected = selectedItems.value?.contains(index) ?: false

                if (item is FolderContent.Data && item.isSelected != selected) {
                    val newItem = FolderContent.Data(item.parent, item.document, selected)
                    finalList.add(newItem)
                } else {
                    finalList.add(item)
                }
            }
        }

        folderItems.value = finalList
    }

    /**
     * Checks if it is pending to finish the select mode. If so, finishes it.
     */
    fun checkSelection() {
        if (isPendingToFinishSelection) {
            isPendingToFinishSelection = false
            finishSelection(true)
            selectedItems.notifyObserver()
        }
    }

    /**
     * Removes all selections.
     */
    fun clearSelected(): List<Int> {
        val positions = mutableListOf<Int>().apply {
            addAll(selectedItems.value!!)
        }

        finishSelection(true)
        return positions
    }

    /**
     * Performs a search in the folder content.
     * If the search is already done, only updates the folder items. If not, requests it.
     *
     * @param newQuery  Text to set as filter.
     */
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

    /**
     * Updates the folder items with the search result and includes it if needed in the map of
     * search results.
     *
     * @param folder        Folder in which the search was carried out.
     * @param query         Text used as filter.
     * @param searchResult  List of the content as the search result.
     */
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

    /**
     * Begins the process to upload the selected or all the current folder items.
     */
    fun upload() {
        getFolderContentUseCase.getRootContentToUpload(
            currentFolder = currentFolder.value!!,
            selectedItems = ArrayList(selectedItems.value!!),
            folderItems = folderItems.value
        ).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error -> logError("Cannot upload anything", error) },
                onSuccess = { results -> checkNameCollisions(results) }
            )
    }

    /**
     * Checks name collisions before starting the upload.
     *
     * @param uploadResults List of UploadFolderResult to upload.
     */
    private fun checkNameCollisions(uploadResults: MutableList<FolderContent.Data>) {
        nameCollisionDisposable =
            checkNameCollisionUseCase.checkFolderUploadList(parentHandle, uploadResults)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onError = { error -> logError("Cannot upload anything", error) },
                    onSuccess = { result ->
                        collisions.value = result.first
                        pendingUploads.addAll(result.second)
                    }
                )
                .addTo(composite)
    }

    /**
     * Proceeds with the upload.
     *
     * @param context               Required for getting absolute path and start the service.
     * @param collisionsResolution  List with the name collisions resolution. Null if is not required.
     */
    fun proceedWithUpload(
        context: Context,
        collisionsResolution: List<NameCollisionResult>? = null
    ) {
        transfersManagement.setIsProcessingFolders(true)
        getContentDisposable = getFolderContentUseCase.getContentToUpload(
            context,
            parentHandle,
            pendingUploads,
            collisionsResolution
        ).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error ->
                    transfersManagement.setIsProcessingFolders(false)

                    if (error is EmptyFolderException) {
                        actionResult.value = getString(R.string.no_uploads_empty_folder)
                        return@subscribeBy
                    } else {
                        logError("Cannot upload anything", error)
                    }
                },
                onSuccess = { uploadResults ->
                    transfersManagement.setIsProcessingFolders(false)
                    actionResult.value =
                        if (uploadResults == 0) null
                        else getQuantityString(
                            R.plurals.upload_began,
                            uploadResults,
                            uploadResults
                        )
                }
            )
            .addTo(composite)
    }

    /**
     * Cancels the current upload process.
     */
    fun cancelUpload() {
        if (transfersManagement.shouldBreakTransfersProcessing()) {
            getContentDisposable?.dispose()
            nameCollisionDisposable?.dispose()
        }
    }
}
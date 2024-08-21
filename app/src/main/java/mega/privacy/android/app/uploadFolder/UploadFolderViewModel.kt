package mega.privacy.android.app.uploadFolder

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.components.textFormatter.TextFormatterUtils.INVALID_INDEX
import mega.privacy.android.app.namecollision.data.NameCollisionResultUiEntity
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.namecollision.NameCollisionChoice
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.file.ApplySortOrderToDocumentFolderUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import mega.privacy.android.domain.usecase.file.GetFilesInDocumentFolderUseCase
import mega.privacy.android.domain.usecase.file.SearchFilesInDocumentFolderRecursiveUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel which manages data of [UploadFolderActivity].
 */
@HiltViewModel
class UploadFolderViewModel @Inject constructor(
    private val getFilesInDocumentFolderUseCase: GetFilesInDocumentFolderUseCase,
    private val applySortOrderToDocumentFolderUseCase: ApplySortOrderToDocumentFolderUseCase,
    private val documentEntityDataMapper: DocumentEntityDataMapper,
    private val searchFilesInDocumentFolderRecursiveUseCase: SearchFilesInDocumentFolderRecursiveUseCase,
    private val checkFileNameCollisionsUseCase: CheckFileNameCollisionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadFolderViewState())
    val uiState = _uiState.asStateFlow()

    private val currentFolder: MutableLiveData<FolderContent.Data> = MutableLiveData()
    private val folderItems: MutableLiveData<MutableList<FolderContent>> = MutableLiveData()
    private val selectedItems: MutableLiveData<MutableList<Int>> = MutableLiveData()
    private val collisions: MutableLiveData<ArrayList<NameCollision>> = MutableLiveData()
    private val actionResult: MutableLiveData<String?> = MutableLiveData()

    private lateinit var parentFolder: String
    private var parentHandle: Long = INVALID_HANDLE
    private var order: SortOrder = SortOrder.ORDER_DEFAULT_ASC
    private var isList: Boolean = true
    var query: String? = null
    private var isPendingToFinishSelection = false
    private var nameCollisionJob: Job? = null
    private var pendingUploads: MutableList<FolderContent.Data> = mutableListOf()
    private val folderTree = mutableMapOf<UriPath, DocumentFolder>()
    private var folderEntities = DocumentFolder(emptyList())

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
        order: SortOrder,
        isList: Boolean,
    ) = viewModelScope.launch {
        parentFolder = documentFile.name.toString()
        currentFolder.value = FolderContent.Data(
            parent = null,
            isFolder = !documentFile.isFile,
            name = documentFile.name.toString(),
            lastModified = documentFile.lastModified(),
            size = documentFile.length(),
            numberOfFiles = 0,
            numberOfFolders = 0,
            uri = documentFile.uri,
        )
        selectedItems.value = mutableListOf()
        this@UploadFolderViewModel.parentHandle = parentHandle
        this@UploadFolderViewModel.order = order
        this@UploadFolderViewModel.isList = isList
        setFolderItems()
    }

    /**
     * Updates the current folder items with the current folder content.
     * If the content is already get, only sets it. If not, requests it.
     */
    private fun setFolderItems() {
        viewModelScope.launch {
            val currentFolder = currentFolder.value ?: return@launch
            val uriPath = UriPath(currentFolder.uri.toString())
            val entities = folderTree[uriPath]
            if (entities != null) {
                folderEntities = entities
            } else {
                runCatching {
                    val folder =
                        getFilesInDocumentFolderUseCase(uriPath)
                    folderTree[uriPath] = folder
                    folderEntities = folder
                }.onFailure {
                    Timber.e(it, "Cannot get folder content")
                }
            }
            applySortAndReorder()
        }
    }

    private fun mapHeaderAndSeparator(
        currentFolder: FolderContent.Data,
        files: List<DocumentEntity>,
        folders: List<DocumentEntity>,
    ): MutableList<FolderContent> {
        val finalItems = mutableListOf<FolderContent>().apply {
            add(FolderContent.Header())
            addAll(folders.map { folder ->
                documentEntityDataMapper(
                    parent = currentFolder,
                    entity = folder
                )
            })
            if (folders.isNotEmpty() && files.isNotEmpty() && !isList) {
                add(FolderContent.Separator())
            }
            addAll(files.map { file ->
                documentEntityDataMapper(
                    parent = currentFolder,
                    entity = file
                )
            })
        }
        return finalItems

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
            currentFolder.value = currentFolder.value?.parent!!
            setFolderItems()
            false
        }

    /**
     * Updates the order.
     *
     * @param newOrder The new order to set.
     */
    fun setOrder(newOrder: SortOrder) {
        viewModelScope.launch {
            if (newOrder != order) {
                order = newOrder
                applySortAndReorder()
            }
        }
    }

    private suspend fun applySortAndReorder() {
        val currentFolder = currentFolder.value ?: return
        runCatching {
            applySortOrderToDocumentFolderUseCase(folderEntities)
        }.onSuccess {
            val (files, folders) = it
            with(mapHeaderAndSeparator(currentFolder, files, folders)) {
                folderItems.value = this
            }
        }.onFailure { error ->
            Timber.e(error, "Cannot apply sort order")
        }
    }

    /**
     * Updates the view type.
     *
     * @param newIsList True if the new view type is list, false if is grid.
     */
    fun setIsList(newIsList: Boolean) {
        viewModelScope.launch {
            if (newIsList != isList) {
                isList = newIsList
                applySortAndReorder()
            }
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
    private fun finishSelection(remove: Boolean = false) = viewModelScope.launch {
        val finalList = mutableListOf<FolderContent>()

        if (remove) {
            selectedItems.value?.clear()
        }

        folderItems.value?.apply {
            for (item in this) {
                val index = indexOf(item)
                val selected = selectedItems.value?.contains(index) ?: false

                if (item is FolderContent.Data && item.isSelected != selected) {
                    val newItem = item.copy(isSelected = selected)
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

    private var searchJob: Job? = null

    /**
     * Performs a search in the folder content.
     * If the search is already done, only updates the folder items. If not, requests it.
     *
     * @param newQuery  Text to set as filter.
     */
    fun search(newQuery: String?) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L.milliseconds) // debounce to avoid user typing too fast
            query = newQuery
            if (newQuery.isNullOrEmpty()) {
                setFolderItems()
            } else {
                val currentFolder = currentFolder.value ?: return@launch
                searchFilesInDocumentFolderRecursiveUseCase(
                    UriPath(currentFolder.uri.toString()),
                    newQuery
                ).catch { error ->
                    Timber.e(error, "Cannot search")
                }.collectLatest { result ->
                    folderEntities = result
                    applySortAndReorder()
                }
            }
        }
    }

    /**
     * Begins the process to upload the selected or all the current folder items.
     */
    fun upload() {
        searchJob?.cancel()
        val currentFolder = currentFolder.value ?: return
        val selectedIndex = selectedItems.value.orEmpty()
        val files = if (selectedIndex.isNotEmpty()) {
            selectedIndex.mapNotNull { index ->
                folderItems.value?.getOrNull(index) as? FolderContent.Data
            }
        } else {
            listOf(currentFolder)
        }
        nameCollisionJob = viewModelScope.launch {
            runCatching {
                checkFileNameCollisionsUseCase(
                    files = files.map {
                        DocumentEntity(
                            name = it.name,
                            size = it.size,
                            lastModified = it.lastModified,
                            uri = UriPath(it.uri.toString()),
                            isFolder = it.isFolder,
                            numFiles = it.numberOfFiles,
                            numFolders = it.numberOfFolders,
                        )
                    },
                    parentNodeId = NodeId(parentHandle)
                )
            }.onSuccess { fileCollisions ->
                pendingUploads.addAll(files)

                if (fileCollisions.isEmpty()) {
                    proceedWithUpload()
                } else {
                    collisions.value = ArrayList(fileCollisions)
                }
            }.onFailure {
                Timber.e(it, "Cannot check name collisions")
            }
        }
    }

    /**
     * Proceeds with the upload.
     *
     * @param collisionsResolution  List with the name collisions resolution. Null if is not required.
     */
    fun proceedWithUpload(
        collisionsResolution: List<NameCollisionResultUiEntity>? = null,
    ) {
        val collisionRename =
            collisionsResolution?.filter { it.choice == NameCollisionChoice.RENAME }
        val pathsAndNames = pendingUploads.associate { folderContentData ->
            val fileName = (collisionRename
                ?.firstOrNull { it.nameCollision.name == folderContentData.name }
                ?.renameName
                    ) ?: folderContentData.name
            folderContentData.uri.toString() to fileName
        }
        _uiState.update { viewState ->
            viewState.copy(
                transferTriggerEvent = triggered(
                    TransferTriggerEvent.StartUpload.Files(
                        pathsAndNames,
                        NodeId(parentHandle)
                    )
                )
            )
        }
    }

    fun consumeTransferTriggerEvent() {
        _uiState.update {
            it.copy(transferTriggerEvent = consumed())
        }
    }
}
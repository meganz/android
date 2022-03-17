package mega.privacy.android.app.uploadFolder.usecase

import android.content.Context
import com.anggrayudi.storage.file.getAbsolutePath
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.components.textFormatter.TextFormatterUtils.INVALID_INDEX
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.exception.EmptyFolderException
import mega.privacy.android.app.namecollision.NameCollisionViewModel
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.uploadFolder.list.data.UploadFolderResult
import mega.privacy.android.app.usecase.CreateFolderUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaNode
import java.io.File
import java.io.FileNotFoundException
import java.lang.NullPointerException
import java.security.InvalidParameterException
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Use case to manage the content of a folder which is going to be uploaded.
 *
 * @property megaApi                    MegaApi required to make the requests to the SDK.
 * @property createFolderUseCase        Use case for creating folders.
 * @property getNodeUseCase             Use case for getting nodes.
 */
class GetFolderContentUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val createFolderUseCase: CreateFolderUseCase,
    private val getNodeUseCase: GetNodeUseCase
) {

    /**
     * Gets the content of a FolderContent Data. It should represent a folder.
     *
     * @param currentFolder Current FolderContent Data from which the content has to be get.
     * @return Single with the list of FolderContent Data representing files and folders.
     */
    private fun get(currentFolder: FolderContent.Data): Single<List<FolderContent.Data>> =
        Single.create { emitter ->
            val listFiles = currentFolder.document.listFiles()
            if (listFiles.isNullOrEmpty()) {
                emitter.onError(FileNotFoundException("Empty folder"))
            } else {
                val folderContentList = mutableListOf<FolderContent.Data>()

                listFiles.forEach { file ->
                    folderContentList.add(FolderContent.Data(currentFolder, file))
                }

                emitter.onSuccess(folderContentList)
            }
        }

    /**
     * Gets the content of a FolderContent Data. It should represent a folder.
     * Reorders the list as per [order] and completes the list with the FolderContent Header and
     * FolderContent Separator if needed.
     *
     * @param currentFolder Current FolderContent Data from which the content has to be get.
     * @param order         Order the list has to fulfill.
     * @param isList        True if the view is list, false if is grid.
     * @return Single with the list of FolderContent representing files, folders, header and separator if needed.
     */
    fun get(
        currentFolder: FolderContent.Data,
        order: Int,
        isList: Boolean
    ): Single<MutableList<FolderContent>> =
        Single.create { emitter ->
            get(currentFolder).blockingSubscribeBy(
                onError = { emitter.onError(FileNotFoundException("Empty folder")) },
                onSuccess = { results ->
                    completeAndReorder(results, order, isList).blockingSubscribeBy(
                        onError = { error -> emitter.onError(error) },
                        onSuccess = { finalContentList -> emitter.onSuccess(finalContentList) }
                    )
                }
            )
        }

    /**
     * Gets the content to upload.
     *
     * @param context       Context required to get the absolute path.
     * @param parentNode    MegaNode in which the content will be uploaded.
     * @param parentFolder  Name of the parent folder of the item.
     * @param folderItem    Item to be managed.
     * @param isSelection   True if only some of the items has to be managed, false otherwise.
     * @return Single with a list of UploadFolderResult.
     */
    private fun getContentToUpload(
        context: Context,
        parentNode: MegaNode,
        parentFolder: String,
        folderItem: FolderContent.Data,
        isSelection: Boolean
    ): Single<ArrayList<UploadFolderResult>> =
        Single.create { emitter ->
            if (folderItem.isFolder) {
                val uris = ArrayList<UploadFolderResult>()

                get(folderItem).blockingSubscribeBy(
                    onError = { error -> emitter.onError(error) },
                    onSuccess = { folderContent ->
                        folderContent.forEach { item ->
                            getContentToUpload(context, parentNode, parentFolder, item, isSelection)
                                .blockingSubscribeBy(
                                    onError = { error -> logWarning("Ignored error", error) },
                                    onSuccess = { urisResult ->
                                        uris.addAll(urisResult)
                                    })
                        }

                        emitter.onSuccess(uris)
                    }
                )
            } else {
                val filePath = folderItem.document.getAbsolutePath(context).split(parentFolder)[1]
                var folderTree = filePath.substring(0, filePath.lastIndexOf(File.separator))

                if (!isSelection) {
                    folderTree = parentFolder + folderTree
                }

                val info = ShareInfo().apply { processUri(folderItem.uri, context) }

                if (folderTree.isEmpty()) {
                    emitter.onSuccess(
                        arrayListOf(
                            UploadFolderResult(
                                info.fileAbsolutePath,
                                folderItem.name!!,
                                folderItem.size,
                                folderItem.lastModified,
                                parentNode.handle
                            )
                        )
                    )
                } else {
                    createFolderUseCase.createTree(parentNode, folderTree).blockingSubscribeBy(
                        onError = { error -> emitter.onError(error) },
                        onSuccess = { parentNodeResult ->
                            emitter.onSuccess(
                                arrayListOf(
                                    UploadFolderResult(
                                        info.fileAbsolutePath,
                                        folderItem.name!!,
                                        folderItem.size,
                                        folderItem.lastModified,
                                        parentNodeResult.handle
                                    )
                                )
                            )
                        }
                    )
                }
            }
        }

    /**
     * Gets the root content to upload.
     *
     * @param currentFolder Name of the parent folder of the item.
     * @param selectedItems List of the selected items' positions if any.
     * @param folderItems   List of the content to be uploaded.
     * @return Single with a list of FolderContent.
     */
    fun getRootContentToUpload(
        currentFolder: FolderContent.Data,
        selectedItems: MutableList<Int>?,
        folderItems: MutableList<FolderContent>?
    ): Single<MutableList<FolderContent.Data>> =
        Single.create { emitter ->
            if (folderItems == null) {
                emitter.onError(EmptyFolderException())
                return@create
            }

            val results = mutableListOf<FolderContent.Data>()

            if (!selectedItems.isNullOrEmpty()) {
                selectedItems.forEach { selected ->
                    if (emitter.isDisposed) {
                        return@create
                    }

                    results.add((folderItems[selected] as FolderContent.Data))
                }
            } else {
                results.add(currentFolder)
            }

            when {
                emitter.isDisposed -> return@create
                results.isEmpty() -> emitter.onError(EmptyFolderException())
                else -> emitter.onSuccess(results)
            }
        }

    /**
     * Gets the content to upload.
     *
     * @param context               Context required to get the absolute path.
     * @param parentNodeHandle      Handle of the parent node in which the content will be uploaded.
     * @param pendingUploads        List of the pending uploads.
     * @param collisionsResolution  List of collisions already resolved.
     * @return Completable
     */
    fun getContentToUpload(
        context: Context,
        parentNodeHandle: Long,
        pendingUploads: List<FolderContent.Data>,
        collisionsResolution: List<NameCollisionResult>?
    ): Single<ArrayList<UploadFolderResult>> =
        Single.create { emitter ->
            val parentNode = getNodeUseCase.get(parentNodeHandle).blockingGetOrNull()

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            val results = ArrayList<UploadFolderResult>()

            pendingUploads.forEach { upload ->

            }

            when {
                emitter.isDisposed -> return@create
                results.isEmpty() -> emitter.onError(FileNotFoundException("Empty folder"))
                else -> emitter.onSuccess(results)
            }
        }

    /**
     * Reorders the list as per [order] and completes it with the FolderContent Header
     * and FolderContent Separator if needed.
     * The FolderContent Separator is only included if the view type is grid.
     *
     * @param folderItems   List of folder content, only FolderContent Data.
     * @param order         Order the list has to fulfill.
     * @param isList        True if the view is list, false if is grid.
     * @return Single with the completed and reordered list.
     */
    private fun completeAndReorder(
        folderItems: List<FolderContent.Data>,
        order: Int,
        isList: Boolean
    ): Single<MutableList<FolderContent>> =
        Single.create { emitter ->
            val results = mutableListOf<FolderContent>().apply {
                add(FolderContent.Header())
                addAll(folderItems)
            }

            reorder(results, order, isList).blockingSubscribeBy(
                onError = { emitter.onError(FileNotFoundException("Empty folder")) },
                onSuccess = { finalSearchList -> emitter.onSuccess(finalSearchList) }
            )
        }

    /**
     * Reorders the list as per [order] and completes it with the a FolderContent Separator if needed.
     * The FolderContent Separator is only included if the view type is grid.
     *
     * @param folderItems   List of folder content, only FolderContent Header abd FolderContent Data.
     * @param order         Order the list has to fulfill.
     * @param isList        True if the view is list, false if is grid.
     * @return Single with the completed and reordered list.
     */
    fun reorder(
        folderItems: MutableList<FolderContent>,
        order: Int,
        isList: Boolean
    ): Single<MutableList<FolderContent>> =
        Single.create { emitter ->
            if (folderItems.isEmpty()) {
                emitter.onError(FileNotFoundException("Empty folder"))
            } else {
                val folders = ArrayList<FolderContent.Data>()
                val files = ArrayList<FolderContent.Data>()
                val finalContent = ArrayList<FolderContent>()
                //Add the header, it has not to be ordered
                finalContent.add(folderItems[0])

                folderItems.forEach { item ->
                    if (item is FolderContent.Data && item.isFolder) {
                        folders.add(item)
                    } else if (item is FolderContent.Data) {
                        files.add(item)
                    }
                }

                when (order) {
                    ORDER_DEFAULT_ASC -> {
                        folders.sortBy { item -> item.name }
                        files.sortBy { item -> item.name }
                    }
                    ORDER_DEFAULT_DESC -> {
                        folders.sortByDescending { item -> item.name }
                        files.sortByDescending { item -> item.name }
                    }
                    ORDER_MODIFICATION_ASC -> {
                        folders.sortBy { item -> item.name }
                        files.sortBy { item -> item.lastModified }
                    }
                    ORDER_MODIFICATION_DESC -> {
                        folders.sortBy { item -> item.name }
                        files.sortByDescending { item -> item.lastModified }
                    }
                    ORDER_SIZE_ASC -> {
                        folders.sortBy { item -> item.name }
                        files.sortBy { item -> item.size }
                    }
                    ORDER_SIZE_DESC -> {
                        folders.sortBy { item -> item.name }
                        files.sortByDescending { item -> item.size }
                    }
                }

                finalContent.addAll(folders)

                if (folders.isNotEmpty() && files.isNotEmpty() && !isList) {
                    finalContent.add(FolderContent.Separator())
                }

                finalContent.addAll(files)
                emitter.onSuccess(finalContent)
            }
        }

    /**
     * Reorders the map as per [order] and completes it with the a FolderContent Separator if needed.
     * The FolderContent Separator is only included if the view type is grid.
     *
     * @param folderContent Map of folder content.
     * @param order         Order the list has to fulfill.
     * @param isList        True if the view is list, false if is grid.
     * @return Single with the completed and reordered map.
     */
    fun reorder(
        folderContent: HashMap<FolderContent.Data?, MutableList<FolderContent>>,
        currentFolder: FolderContent.Data?,
        order: Int,
        isList: Boolean
    ): Single<HashMap<FolderContent.Data?, MutableList<FolderContent>>> =
        Single.create { emitter ->
            if (folderContent.isEmpty()) {
                emitter.onError(FileNotFoundException("Empty folder content"))
                return@create
            }

            folderContent.forEach { item ->
                if (currentFolder != null && item.key != currentFolder) {
                    reorder(item.value, order, isList).blockingSubscribeBy(
                        onError = { error -> logWarning("Ignored error", error) },
                        onSuccess = { finalContentList ->
                            folderContent[item.key] = finalContentList
                        }
                    )
                }
            }

            emitter.onSuccess(folderContent)
        }

    /**
     * Adds or removes the FolderContent Separator in the list depending on if the current view type
     * is list or grid.
     *
     * @param folderItems   List of folder content.
     * @param isList        True if the view type is list, false if is grid.
     * @return Single with the updated list.
     */
    fun switchView(
        folderItems: MutableList<FolderContent>,
        isList: Boolean
    ): Single<MutableList<FolderContent>> =
        Single.create { emitter ->
            if (folderItems.isEmpty()) {
                emitter.onError(FileNotFoundException("Empty folder content."))
                return@create
            }

            val isFirstItemFolder =
                folderItems[1] is FolderContent.Data && (folderItems[1] as FolderContent.Data).isFolder
            val lastIndex = folderItems.size - 1
            val isLastItemFile =
                folderItems[lastIndex] is FolderContent.Data && !(folderItems[lastIndex] as FolderContent.Data).isFolder

            if (!isFirstItemFolder || !isLastItemFile) {
                emitter.onError(InvalidParameterException("There are no folders, no update needed."))
                return@create
            }

            var index = INVALID_INDEX

            for ((i, item) in folderItems.withIndex()) {
                if (isList && item is FolderContent.Separator) {
                    index = i
                    break
                } else if (!isList && item is FolderContent.Data && !item.isFolder) {
                    index = i
                    break
                }
            }

            if (index == INVALID_INDEX) {
                emitter.onError(InvalidParameterException("Cannot perform update, no index found."))
                return@create
            }

            if (isList) {
                folderItems.removeAt(index)
            } else {
                folderItems.add(index, FolderContent.Separator())
            }

            emitter.onSuccess(folderItems)
        }

    /**
     * Adds or removes the FolderContent Separator in the map depending on if the current view type
     * is list or grid.
     *
     * @param folderContent Map of folder content.
     * @param currentFolder Current folder to avoid processing it again.
     * @param isList        True if the view type is list, false if is grid.
     * @return Single with the updated map.
     */
    fun switchView(
        folderContent: HashMap<FolderContent.Data?, MutableList<FolderContent>>,
        currentFolder: FolderContent.Data?,
        isList: Boolean
    ): Single<HashMap<FolderContent.Data?, MutableList<FolderContent>>> =
        Single.create { emitter ->
            if (folderContent.isEmpty()) {
                emitter.onError(FileNotFoundException("Empty folder content"))
                return@create
            }

            folderContent.forEach { item ->
                if (currentFolder != null && item.key != currentFolder) {
                    switchView(item.value, isList).blockingSubscribeBy(
                        onError = { error -> logWarning("Ignored error", error) },
                        onSuccess = { finalContentList ->
                            folderContent[item.key] = finalContentList
                        }
                    )
                }
            }

            emitter.onSuccess(folderContent)
        }

    /**
     * Filters the folder content by name as per the received [query].
     *
     * @param query         Text which has to appear in all the search results.
     * @param currentFolder Current folder in which the search has to be carried out.
     * @return Single with the list of filtered FolderContent Data representing files and folders.
     */
    private fun search(
        query: String,
        currentFolder: FolderContent.Data
    ): Single<MutableList<FolderContent.Data>> =
        Single.create { emitter ->
            val searchResults = mutableListOf<FolderContent.Data>()

            get(currentFolder).blockingSubscribeBy(
                onError = { emitter.onError(FileNotFoundException("Empty folder")) },
                onSuccess = { folderContentList ->
                    folderContentList.forEach { item ->
                        if (item.isFolder) {
                            search(query, item).blockingSubscribeBy(
                                onError = { error -> logWarning("Ignored error", error) },
                                onSuccess = { results -> searchResults.addAll(results) }
                            )
                        }
                    }

                    searchResults.addAll(folderContentList.filter {
                        it.name?.lowercase(Locale.getDefault())
                            ?.contains(query.lowercase(Locale.getDefault())) == true
                    })
                }
            )

            if (searchResults.isEmpty()) {
                emitter.onError(FileNotFoundException("Empty search"))
            } else {
                emitter.onSuccess(searchResults)
            }
        }

    /**
     * Filters the folder content by name as per the received [query].
     *
     * @param query         Text which has to appear in all the search results.
     * @param currentFolder Current folder in which the search has to be carried out.
     * @param order         Order the list has to fulfill.
     * @param isList        True if the view type is list, false if is grid.
     * @return Single with the filtered list of FolderContent representing files, folders, header and separator if needed.
     */
    fun search(
        query: String,
        currentFolder: FolderContent.Data,
        order: Int,
        isList: Boolean
    ): Single<MutableList<FolderContent>> =
        Single.create { emitter ->
            search(query, currentFolder).blockingSubscribeBy(
                onError = { error -> emitter.onError(error) },
                onSuccess = { results ->
                    completeAndReorder(results, order, isList).blockingSubscribeBy(
                        onError = { emitter.onError(FileNotFoundException("Empty search")) },
                        onSuccess = { finalSearchList -> emitter.onSuccess(finalSearchList) }
                    )
                })
        }
}
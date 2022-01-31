package mega.privacy.android.app.uploadFolder.usecase

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.components.textFormatter.TextFormatterUtils.INVALID_INDEX
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import nz.mega.sdk.MegaApiJava.*
import java.io.FileNotFoundException
import java.security.InvalidParameterException
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class GetFolderContentUseCase @Inject constructor() {

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
                        onSuccess = { finalContentList ->
                            folderContent[item.key] = finalContentList
                        }
                    )
                }
            }

            emitter.onSuccess(folderContent)
        }

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
                        onSuccess = { finalContentList ->
                            folderContent[item.key] = finalContentList
                        }
                    )
                }
            }

            emitter.onSuccess(folderContent)
        }

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
package mega.privacy.android.app.upload.usecase

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.upload.list.data.FolderContent
import nz.mega.sdk.MegaApiJava.*
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlin.collections.ArrayList

class GetFolderContentUseCase @Inject constructor() {

    fun get(folderContent: FolderContent.Data, order: Int): Single<List<FolderContent>> =
        Single.create { emitter ->
            val listFiles = folderContent.document.listFiles()
            if (listFiles.isNullOrEmpty()) {
                emitter.onError(FileNotFoundException("Empty folder"))
            } else {
                val folderContentList = ArrayList<FolderContent>()
                folderContentList.add(FolderContent.Header())

                listFiles.forEach { file ->
                    folderContentList.add(FolderContent.Data(folderContent, file))
                }

                reorder(folderContentList, order).blockingSubscribeBy(
                    onError = { emitter.onError(FileNotFoundException("Empty folder")) },
                    onSuccess = { finalContentList -> emitter.onSuccess(finalContentList) }
                )
            }
        }

    fun reorder(folderContent: List<FolderContent>, order: Int): Single<List<FolderContent>> =
        Single.create { emitter ->
            if (folderContent.isEmpty()) {
                emitter.onError(FileNotFoundException("Empty folder"))
            } else {
                val folders = ArrayList<FolderContent.Data>()
                val files = ArrayList<FolderContent.Data>()
                val finalContent = ArrayList<FolderContent>()
                //Add the header, it has not to be ordered
                finalContent.add(folderContent[0])

                folderContent.forEach { item ->
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
                finalContent.add(FolderContent.Separator())
                finalContent.addAll(files)
                emitter.onSuccess(finalContent)
            }
        }
}
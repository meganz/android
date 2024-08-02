package mega.privacy.android.app.uploadFolder.usecase

import android.content.Context
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import kotlinx.coroutines.rx3.rxSingle
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.domain.entity.node.namecollision.NameCollisionChoice
import mega.privacy.android.app.namecollision.data.NameCollisionResultUiEntity
import mega.privacy.android.app.uploadFolder.DocumentEntityDataMapper
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.uploadFolder.list.data.UploadFolderResult
import mega.privacy.android.app.usecase.UploadUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.FolderNameNullException
import mega.privacy.android.domain.usecase.file.GetFilesInDocumentFolderUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.DoesNodeExistUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to manage the content of a folder which is going to be uploaded.
 */
class GetFolderContentUseCase @Inject constructor(
    private val doesNodeExistUseCase: DoesNodeExistUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val createFolderNodeUseCase: CreateFolderNodeUseCase,
    private val uploadUseCase: UploadUseCase,
    private val getFilesInDocumentFolderUseCase: GetFilesInDocumentFolderUseCase,
    private val documentEntityDataMapper: DocumentEntityDataMapper,
) {

    /**
     * Gets the content of a FolderContent Data. It should represent a folder.
     *
     * @param currentFolder Current FolderContent Data from which the content has to be get.
     * @return Single with the list of FolderContent Data representing files and folders.
     */
    private fun get(
        currentFolder: FolderContent.Data,
    ): Single<List<FolderContent.Data>> = rxSingle {
        getFilesInDocumentFolderUseCase(UriPath(currentFolder.uri.toString()))
    }.map {
        it.files.mapNotNull { file ->
            documentEntityDataMapper(currentFolder, file)
        }
    }

    /**
     * Gets the content to upload.
     *
     * @param context       Context required to get the absolute path.
     * @param parentNodeId  NodeId in which the content will be uploaded.
     * @param folderItem    Item to be managed.
     * @param renameName    A valid name if the file has to be uploaded with a different name, null otherwise.
     * @return Single with a list of UploadFolderResult.
     */
    private fun getContentToUpload(
        context: Context,
        parentNodeId: NodeId,
        folderItem: FolderContent.Data,
        renameName: String? = null,
    ): Single<ArrayList<UploadFolderResult>> =
        Single.create { emitter ->
            val folderName = folderItem.name

            if (folderName == null) {
                emitter.onError(FolderNameNullException())
                return@create
            }

            if (folderItem.isFolder) {
                val newParentNodeId = rxSingle {
                    getChildNodeUseCase(parentNodeId, folderName)?.id
                        ?: createFolderNodeUseCase(folderName, parentNodeId)
                }.blockingGetOrNull()

                if (emitter.isDisposed) {
                    return@create
                }
                if (newParentNodeId == null) {
                    emitter.onError(MegaNodeException.ParentDoesNotExistException())
                    return@create
                }

                val results = ArrayList<UploadFolderResult>()

                get(folderItem).blockingSubscribeBy(
                    onError = { error -> emitter.onError(error) },
                    onSuccess = { folderContent ->
                        folderContent.forEach { item ->
                            getContentToUpload(context, newParentNodeId, item)
                                .blockingSubscribeBy(
                                    onError = { error ->
                                        Timber.w(
                                            error,
                                            "Ignored error"
                                        )
                                    },
                                    onSuccess = { urisResult ->
                                        results.addAll(urisResult)
                                    })
                        }

                        emitter.onSuccess(results)
                    }
                )
            } else {
                val info = ShareInfo().apply { processUri(null, folderItem.uri, context) }

                emitter.onSuccess(
                    arrayListOf(
                        UploadFolderResult(
                            absolutePath = info.fileAbsolutePath,
                            name = folderName,
                            size = folderItem.size,
                            lastModified = folderItem.lastModified,
                            parentHandle = parentNodeId.longValue,
                            renameName = renameName
                        )
                    )
                )
            }
        }

    /**
     * Gets the content to upload.
     *
     * @param context               Context required to get the absolute path.
     * @param parentNodeId      Handle of the parent node in which the content will be uploaded.
     * @param pendingUploads        List of the pending uploads.
     * @param collisionsResolution  List of collisions already resolved.
     * @return Single with the size of uploaded items.
     */
    fun getContentToUpload(
        context: Context,
        parentNodeId: NodeId,
        pendingUploads: List<FolderContent.Data>,
        collisionsResolution: List<NameCollisionResultUiEntity>?,
    ): Single<Int> =
        Single.create { emitter ->
            val parentNodeExists =
                rxSingle { doesNodeExistUseCase(parentNodeId) }.blockingGetOrNull()

            if (emitter.isDisposed) {
                return@create
            }

            if (parentNodeExists != true) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            val uploadResults = mutableListOf<UploadFolderResult>()

            if (collisionsResolution.isNullOrEmpty()) {
                pendingUploads.forEach { upload ->
                    if (emitter.isDisposed) {
                        return@create
                    }

                    getContentToUpload(
                        context = context,
                        parentNodeId = parentNodeId,
                        folderItem = upload
                    ).blockingSubscribeBy(
                        onError = { error -> Timber.w(error, "Ignored error") },
                        onSuccess = { result -> uploadResults.addAll(result) }
                    )
                }
            } else {
                val collisions = collisionsResolution.toMutableList()
                pendingUploads.forEach { upload ->
                    if (emitter.isDisposed) {
                        return@create
                    }

                    if (upload.nameCollision != null) {
                        for (collision in collisions) {
                            if (emitter.isDisposed) {
                                return@create
                            }

                            if (upload.nameCollision == collision.nameCollision
                                && collision.choice != NameCollisionChoice.CANCEL
                            ) {
                                val renameName =
                                    if (collision.choice == NameCollisionChoice.RENAME) {
                                        collision.renameName
                                    } else {
                                        null
                                    }

                                getContentToUpload(
                                    context = context,
                                    parentNodeId = parentNodeId,
                                    folderItem = upload,
                                    renameName
                                ).blockingSubscribeBy(
                                    onError = { error ->
                                        Timber.w(error, "Ignored error")
                                    },
                                    onSuccess = { result -> uploadResults.addAll(result) }
                                )

                                collisions.remove(collision)
                                break
                            }
                        }
                    } else {
                        getContentToUpload(
                            context = context,
                            parentNodeId = parentNodeId,
                            folderItem = upload
                        ).blockingSubscribeBy(
                            onError = { error -> Timber.w(error, "Ignored error") },
                            onSuccess = { result -> uploadResults.addAll(result) }
                        )
                    }
                }
            }


            when {
                emitter.isDisposed -> return@create
                uploadResults.isEmpty() -> emitter.onSuccess(0)
                else -> {
                    for (result in uploadResults) {
                        uploadUseCase.upload(context, result).blockingSubscribeBy(
                            onError = { error -> emitter.onError(error) }
                        )
                    }

                    when {
                        emitter.isDisposed -> return@create
                        else -> emitter.onSuccess(uploadResults.size)
                    }
                }
            }
        }

    private fun <T : Any> Single<T>.blockingGetOrNull(): T? =
        try {
            blockingGet()
        } catch (ignore: Exception) {
            null
        }
}

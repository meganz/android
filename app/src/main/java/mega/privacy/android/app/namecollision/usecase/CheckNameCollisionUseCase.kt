package mega.privacy.android.app.namecollision.usecase

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.domain.exception.EmptyFolderException
import mega.privacy.android.app.uploadFolder.list.data.UploadFolderResult
import mega.privacy.android.app.usecase.exception.MegaNodeException
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.util.ArrayList
import javax.inject.Inject

/**
 * Use case for checking name collisions before uploading, copying or moving.
 */
class CheckNameCollisionUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Checks if a node with the given name exists on the provided parent node.
     *
     * @param name          Name of the node.
     * @param parentHandle  Handle of the parent node in which to look.
     * @return Single Long with the node handle with which there is a name collision.
     */
    fun check(name: String, parentHandle: Long): Single<Long> =
        Single.create { emitter ->
            val parentNode = if (parentHandle == INVALID_HANDLE) {
                megaApi.rootNode
            } else {
                megaApi.getNodeByHandle(parentHandle)
            }

            check(name, parentNode).blockingSubscribeBy(
                onError = { error -> emitter.onError(error) },
                onSuccess = { handle -> emitter.onSuccess(handle) }
            )
        }

    /**
     * Checks if a node with the given name exists on the provided parent node.
     *
     * @param name          Name of the node.
     * @param parentNode    Parent node in which to look.
     * @return Single Long with the node handle with which there is a name collision.
     */
    fun check(name: String, parentNode: MegaNode?): Single<Long> =
        Single.create { emitter ->
            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            val child = megaApi.getChildNode(parentNode, name)

            if (child != null) {
                emitter.onSuccess(child.handle)
            } else {
                emitter.onError(MegaNodeException.ChildDoesNotExistsException())
            }
        }

    /**
     * Checks a list of ShareInfo in order to know which names already exist
     * on the provided parent node.
     *
     * @param shareInfos    List of ShareInfo to check.
     * @param parentNode    Parent node in which to look.
     * @return Single<Pair<List<ShareInfo>, List<ShareInfo>>> containing:
     *  - First:    List of NameCollision with name collisions.
     *  - Second:   List of ShareInfo without name collision.
     */
    fun check(
        shareInfos: List<ShareInfo>,
        parentNode: MegaNode?
    ): Single<Pair<ArrayList<NameCollision>, List<ShareInfo>>> =
        Single.create { emitter ->
            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            val collisions = ArrayList<NameCollision>()
            val results = ArrayList<ShareInfo>()

            for (shareInfo in shareInfos) {
                check(shareInfo.originalFileName, parentNode).blockingSubscribeBy(
                    onError = { error ->
                        if (error is MegaNodeException.ParentDoesNotExistException) {
                            emitter.onError(error)
                            return@blockingSubscribeBy
                        } else {
                            results.add(shareInfo)
                        }
                    },
                    onSuccess = { handle ->
                        collisions.add(
                            NameCollision.Upload.getUploadCollision(
                                handle,
                                shareInfo,
                                parentNode.handle
                            )
                        )
                    },
                )
            }

            emitter.onSuccess(Pair(collisions, results))
        }

    /**
     * Checks a list of UploadFolderResult in order to know which names already exist
     * on the provided parent node.
     *
     * @param uploadResults    List of UploadFolderResult to check.
     * @return Single<Pair<List<UploadFolderResult>, List<UploadFolderResult>>> containing:
     *  - First:    List of NameCollision with name collisions.
     *  - Second:   List of UploadFolderResult without name collision.
     */
    fun check(
        uploadResults: List<UploadFolderResult>
    ): Single<Pair<ArrayList<NameCollision>, List<UploadFolderResult>>> =
        Single.create { emitter ->
            if (uploadResults.isEmpty()) {
                emitter.onError(EmptyFolderException())
                return@create
            }

            if (emitter.isDisposed) {
                return@create
            }

            val collisions = ArrayList<NameCollision>()
            val results = ArrayList<UploadFolderResult>()

            for (uploadResult in uploadResults) {
                if (emitter.isDisposed) {
                    return@create
                }

                check(uploadResult.name, uploadResult.parentHandle).blockingSubscribeBy(
                    onError = { error ->
                        if (error is MegaNodeException.ParentDoesNotExistException) {
                            emitter.onError(error)
                            return@blockingSubscribeBy
                        } else {
                            results.add(uploadResult)
                        }
                    },
                    onSuccess = { handle ->
                        collisions.add(
                            NameCollision.Upload.getUploadCollision(
                                handle,
                                uploadResult
                            )
                        )
                    },
                )
            }

            when {
                emitter.isDisposed -> return@create
                else -> emitter.onSuccess(Pair(collisions, results))
            }
        }
}
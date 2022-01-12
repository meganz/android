package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.usecase.exception.MegaNodeException
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import javax.inject.Inject

class CheckNameCollisionUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Checks if a node with the given name exists on the provided parent node.
     *
     * @param name          Name of the node.
     * @param parentHandle  Handle of the parent node in which to look.
     * @return Completable
     */
    fun check(name: String, parentHandle: Long): Completable =
        Completable.create { emitter ->
            val parentNode = if (parentHandle == INVALID_HANDLE) {
                megaApi.rootNode
            } else {
                megaApi.getNodeByHandle(parentHandle)
            }

            check(name, parentNode).blockingSubscribeBy(
                onError = { error -> emitter.onError(error) },
                onComplete = { emitter.onComplete() }
            )
        }

    /**
     * Checks if a node with the given name exists on the provided parent node.
     *
     * @param name          Name of the node.
     * @param parentNode  Handle of the parent node in which to look.
     * @return Completable
     */
    fun check(name: String, parentNode: MegaNode?): Completable =
        Completable.create { emitter ->
            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            val child = megaApi.getChildNode(parentNode, name)

            if (child != null) {
                emitter.onComplete()
            } else {
                emitter.onError(MegaNodeException.ChildAlreadyExistsException())
            }
        }
}
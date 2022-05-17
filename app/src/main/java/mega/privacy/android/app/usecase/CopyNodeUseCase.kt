package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for copying MegaNodes.
 *
 * @property megaApi MegaApiAndroid instance to move nodes..
 */
class CopyNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getNodeUseCase: GetNodeUseCase
) {

    /**
     * Copy node to a different location, either passing handles or node itself.
     *
     * @param nodeHandle        Node handle to be copied
     * @param toParentHandle    Parent node handle to be copied to
     * @param node              Node to be copied
     * @param toParentNode      Parent node to be copied to
     * @return                  Completable
     */
    fun copy(
        nodeHandle: Long? = null,
        toParentHandle: Long? = null,
        node: MegaNode? = null,
        toParentNode: MegaNode? = null
    ): Completable =
        Completable.fromCallable {
            require((node != null || nodeHandle != null) && (toParentNode != null || toParentHandle != null))
            copy(
                node ?: getNodeUseCase.get(nodeHandle!!).blockingGet(),
                toParentNode ?: getNodeUseCase.get(toParentHandle!!).blockingGet()
            ).blockingAwait()
        }

    /**
     * Copy node to a different location.
     *
     * @param currentNode   Node to be copied
     * @param toParentNode  Parent node to be copied to
     * @return              Completable
     */
    fun copy(currentNode: MegaNode?, toParentNode: MegaNode?): Completable =
        Completable.create { emitter ->
            if (currentNode == null || toParentNode == null) {
                emitter.onError(IllegalArgumentException("Null nodes"))
                return@create
            }

            megaApi.copyNode(currentNode, toParentNode, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        emitter.onComplete()
                    } else {
                        emitter.onError(error.toMegaException())
                    }
                }
            ))
        }
}

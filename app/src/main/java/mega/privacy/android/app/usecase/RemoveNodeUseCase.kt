package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.data.RemoveRequestResult
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for removing MegaNodes.
 *
 * @property megaApi MegaApiAndroid instance to move nodes..
 */
class RemoveNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getNodeUseCase: GetNodeUseCase
) {

    /**
     * Removes a node.
     *
     * @param nodeHandle    Node handle to be removed
     * @return              Completable
     */
    fun remove(nodeHandle: Long): Completable =
        getNodeUseCase.get(nodeHandle).flatMapCompletable { node -> remove(node) }

    /**
     * Removes a node.
     *
     * @param node  The MegaNode to be removed.
     * @return      Completable.
     */
    fun remove(node: MegaNode): Completable =
        Completable.create { emitter ->
            megaApi.remove(
                node,
                OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == API_OK) {
                        emitter.onComplete()
                    } else {
                        emitter.onError(error.toMegaException())
                    }
                })
            )
        }

    /**
     * Removes a list of MegaNodes.
     *
     * @param handles   List of MegaNode handles to remove.
     * @return The removal result.
     */
    fun remove(handles: List<Long>): Single<RemoveRequestResult> =
        Single.create { emitter ->
            var errorCount = 0

            handles.forEach { handle ->
                val node = getNodeUseCase.get(handle).blockingGetOrNull()

                if (node == null) {
                    errorCount++
                } else {
                    remove(node).blockingSubscribeBy(onError = {
                        errorCount++
                    })
                }
            }

            emitter.onSuccess(
                RemoveRequestResult(
                    count = handles.size,
                    errorCount
                ).apply { resetAccountDetailsIfNeeded() }
            )
        }
}
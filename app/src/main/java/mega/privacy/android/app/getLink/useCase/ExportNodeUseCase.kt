package mega.privacy.android.app.getLink.useCase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.exception.toMegaException
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for export nodes.
 *
 * @property megaApi MegaApiAndroid instance to use.
 */
class ExportNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getNodeUseCase: GetNodeUseCase
) {

    /**
     * Generate a temporary public link of a file/folder node.
     *
     * @param nodeHandle MegaNode handle to export.
     * @param expireTime The time to set as expiry date.
     * @return Single<String> The link if the request finished with success, error if not.
     */
    fun export(nodeHandle: Long, expireTime: Long? = null): Single<String> =
        getNodeUseCase.get(nodeHandle).flatMap { export(it, expireTime) }

    /**
     * Generate temporary public links for a list of file/folder nodes.
     *
     * @param nodes List of nodes to export.
     * @return Single<String> The links if the request finished with success, error if not.
     */
    fun export(nodes: List<MegaNode>): Single<HashMap<Long, String>> =
        Single.fromCallable {
            val result = hashMapOf<Long, String>()
            nodes.forEach { node ->
                result[node.handle] = export(node).blockingGet()
            }
            result
        }

    /**
     * Generate a temporary public link of a file/folder node.
     *
     * @param node MegaNode to export.
     * @param expireTime The time to set as expiry date.
     * @return Single<String> The link if the request finished with success, error if not.
     */
    fun export(node: MegaNode?, expireTime: Long? = null): Single<String> =
        Single.create { emitter ->
            if (node == null || node.isTakenDown) {
                emitter.onError(IllegalArgumentException("Not available node"))
                return@create
            }

            if (node.isExported && !node.isExpired && node.expirationTime == expireTime) {
                emitter.onSuccess(node.publicLink)
                return@create
            }

            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    when {
                        emitter.isDisposed -> return@OptionalMegaRequestListenerInterface
                        error.errorCode == MegaError.API_OK -> emitter.onSuccess(request.link)
                        else -> emitter.onError(error.toMegaException())
                    }
                }
            )

            if (expireTime != null && expireTime > 0) {
                megaApi.exportNode(node, expireTime.toInt(), listener)
            } else {
                megaApi.exportNode(node, listener)
            }
        }

    /**
     * Launches a request to stop sharing a file/folder
     *
     * @param nodeHandle    MegaNode handle to stop sharing
     * @return              Completable subscription
     */
    fun disableExport(nodeHandle: Long): Completable =
        getNodeUseCase.get(nodeHandle).flatMapCompletable(::disableExport)

    /**
     * Launches a request to stop sharing a file/folder
     *
     * @param node          MegaNode to stop sharing
     * @return              Completable subscription
     */
    fun disableExport(node: MegaNode?): Completable =
        Completable.create { emitter ->
            if (node == null) {
                emitter.onError(IllegalArgumentException("Null node"))
                return@create
            }

            megaApi.disableExport(node, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when {
                        emitter.isDisposed -> return@OptionalMegaRequestListenerInterface
                        error.errorCode == MegaError.API_OK -> emitter.onComplete()
                        else -> emitter.onError(error.toMegaException())
                    }
                }
            ))
        }
}

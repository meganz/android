package mega.privacy.android.app.getLink.useCase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.errors.BusinessAccountOverdueMegaError
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
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
    @MegaApi private val megaApi: MegaApiAndroid
) {
    /**
     * Launches a request to export a node.
     *
     * @param nodeHandle MegaNode handle to export.
     * @return Single<String> The link if the request finished with success, error if not.
     */
    fun export(nodeHandle: Long): Single<String> =
        Single.create { emitter ->
            val node = megaApi.getNodeByHandle(nodeHandle)
            megaApi.exportNode(node, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        emitter.onSuccess(request.link)
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    /**
     * Launches a request to get the link of a node with an expiry date.
     *
     * @param node       MegaNode to export.
     * @param expiryTime The time to set as expiry date.
     * @return Single<String> The link if the request finished with success, error if not.
     */
    fun exportWithTimestamp(node: MegaNode, expiryTime: Int): Single<String> =
        Single.create { emitter ->
            megaApi.exportNode(node, expiryTime, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        emitter.onSuccess(request.link)
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }

    /**
     * Launches a request to export a list of nodes.
     *
     * @param nodes List of nodes to export.
     * @return Single<String> The links if the request finished with success, error if not.
     */
    fun export(nodes: List<MegaNode>): Single<HashMap<Long, String>> =
        Single.create { emitter ->
            val exported = HashMap<Long, String>()
            var pending = nodes.size

            for (node in nodes) {
                megaApi.exportNode(node, OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        pending--

                        if (error.errorCode == MegaError.API_OK) {
                            exported[request.nodeHandle] = request.link
                        } else {
                            logError("Error exporting ${node.handle}")
                        }

                        if (pending == 0) {
                            if (exported.isNotEmpty()) {
                                emitter.onSuccess(exported)
                            } else {
                                emitter.onError(error.toThrowable())
                            }
                        }
                    }
                ))
            }
        }

    /**
     * Launches a request to stop sharing a file/folder
     *
     * @param nodeHandle    MegaNode to stop sharing
     * @return              Completable subscription
     */
    fun disableExport(nodeHandle: Long): Completable =
        Completable.create { emitter ->
            val node = megaApi.getNodeByHandle(nodeHandle)
            megaApi.disableExport(node, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        MegaError.API_OK ->
                            emitter.onComplete()
                        MegaError.API_EBUSINESSPASTDUE ->
                            emitter.onError(BusinessAccountOverdueMegaError())
                        else ->
                            emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}

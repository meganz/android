package mega.privacy.android.app.getLink.useCase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import javax.inject.Inject

class ExportNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {
    /**
     * Launches a request to get the link of a node.
     *
     * @param node MegaNode to export.
     * @return Single<String> The link if the request finished with success, error if not.
     */
    fun export(node: MegaNode): Single<String> =
        Single.create { emitter ->
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
}
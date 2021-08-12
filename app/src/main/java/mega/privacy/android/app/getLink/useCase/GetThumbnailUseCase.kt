package mega.privacy.android.app.getLink.useCase

import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.FileUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject

class GetThumbnailUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {
    /**
     * Launches a request to get the thumbnails of a list of nodes.
     *
     * @param nodes List of nodes to get their thumbnail.
     * @return Single<String> The encrypted link if the request finished with success, error if not.
     */
    fun get(nodes: List<MegaNode>, thumbFolder: File): Flowable<Long> =
        Flowable.create({ emitter ->
            for (node in nodes) {
                val thumbnail = File(thumbFolder, node.base64Handle + FileUtil.JPG_EXTENSION)

                megaApi.getThumbnail(node,
                    thumbnail.absolutePath,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (error.errorCode == MegaError.API_OK && thumbnail.exists()) {
                                emitter.onNext(request.nodeHandle)
                            } else {
                                emitter.onError(error.toThrowable())
                            }
                        }
                    ))
            }
        }, BackpressureStrategy.LATEST)
}
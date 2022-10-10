package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.megachat.AndroidMegaRichLinkMessage
import mega.privacy.android.app.usecase.exception.toMegaException
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

/**
 * Use case for getting the public node from a file link.
 */
class GetPublicNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Gets file link as [AndroidMegaRichLinkMessage].
     *
     * @param link The file link.
     * @return [AndroidMegaRichLinkMessage] containing the file link and its public node.
     */
    fun get(link: String): Single<AndroidMegaRichLinkMessage> =
        Single.create { emitter ->
            megaApi.getPublicNode(
                link,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    val publicNode = request.publicMegaNode

                    when {
                        emitter.isDisposed -> {
                            return@OptionalMegaRequestListenerInterface
                        }
                        error.errorCode == API_OK && publicNode != null -> {
                            emitter.onSuccess(AndroidMegaRichLinkMessage(link, publicNode))
                        }
                        else -> {
                            emitter.onError(error.toMegaException())
                        }
                    }
                })
            )
        }
}
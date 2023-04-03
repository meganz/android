package mega.privacy.android.app.usecase

import android.content.Context
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.megachat.AndroidMegaRichLinkMessage
import mega.privacy.android.app.usecase.exception.toMegaException
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError.API_OK
import javax.inject.Inject

/**
 * Use case for getting folder link information without logging and without fetching nodes.
 */
class GetPublicLinkInformationUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) {

    /**
     * Gets folder link information.
     *
     * @param link The folder link.
     * @return AndroidMegaRichLinkMessage containing the folder link info.
     */
    fun get(link: String, context: Context): Single<AndroidMegaRichLinkMessage> =
        Single.create { emitter ->
            megaApi.getPublicLinkInformation(
                link,
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    when {
                        emitter.isDisposed -> {
                            return@OptionalMegaRequestListenerInterface
                        }
                        error.errorCode == API_OK -> {
                            val folderInfo = request.megaFolderInfo
                            val folderContent =
                                mega.privacy.android.app.utils.TextUtil.getFolderInfo(
                                    folderInfo.numFolders,
                                    folderInfo.numFiles,
                                    context,
                                )

                            emitter.onSuccess(
                                AndroidMegaRichLinkMessage(
                                    link,
                                    folderContent,
                                    request.text
                                )
                            )
                        }
                        else -> {
                            emitter.onError(error.toMegaException())
                        }
                    }
                })
            )
        }
}
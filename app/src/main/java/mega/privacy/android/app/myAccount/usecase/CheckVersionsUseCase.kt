package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaFolderInfo
import javax.inject.Inject

class CheckVersionsUseCase  @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val myAccountInfo: MyAccountInfo
) {

    /**
     * Launches a request to check versions and updates MyAccountInfo if finishes with success.
     *
     * @return Completable onComplete() if the request finished with success, error if not.
     */
    fun check(): Completable =
        Completable.create { emitter ->
            megaApi.getFolderInfo(megaApi.rootNode, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        val info: MegaFolderInfo = request.megaFolderInfo
                        myAccountInfo.numVersions = info.numVersions
                        myAccountInfo.previousVersionsSize = info.versionsSize
                        emitter.onComplete()
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}
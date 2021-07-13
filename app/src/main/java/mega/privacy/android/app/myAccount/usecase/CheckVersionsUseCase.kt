package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
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

    fun check(): Single<Boolean> =
        Single.create { emitter ->
            megaApi.getFolderInfo(megaApi.rootNode, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        val info: MegaFolderInfo = request.megaFolderInfo
                        myAccountInfo.numVersions = info.numVersions
                        myAccountInfo.previousVersionsSize = info.versionsSize
                        emitter.onSuccess(true)
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}
package mega.privacy.android.app.myAccount.usecase

import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import javax.inject.Inject

class GetFileVersionsOptionUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Launches a request to check file versions option and updates value if finishes with success.
     *
     * @return Completable onComplete() if the request finished with success, error if not.
     */
    fun get(): Completable =
        Completable.create { emitter ->
            megaApi.getFileVersionsOption(OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isDisposed) {
                        return@OptionalMegaRequestListenerInterface
                    }

                    if (error.errorCode == MegaError.API_OK) {
                        MegaApplication.setDisableFileVersions(request.flag)
                        emitter.onComplete()
                    } else {
                        emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}
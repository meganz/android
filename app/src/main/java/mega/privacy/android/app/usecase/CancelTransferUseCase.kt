package mega.privacy.android.app.usecase

import android.util.Log
import io.reactivex.rxjava3.core.Completable
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import javax.inject.Inject

class CancelTransferUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    fun cancel(transferTag: Int): Completable =
        Completable.create { emitter ->
            Log.wtf("CACATAG", "cancelTransferByTag: $transferTag")
            megaApi.cancelTransferByTag(transferTag, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    when (error.errorCode) {
                        MegaError.API_OK ->
                            emitter.onComplete()
                        else ->
                            emitter.onError(error.toThrowable())
                    }
                }
            ))
        }
}

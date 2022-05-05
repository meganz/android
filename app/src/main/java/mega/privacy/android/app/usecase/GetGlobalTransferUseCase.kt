package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to subscribe to global events related to MegaApi.
 *
 * @property megaApi    MegaApi required to call the SDK
 */
@Singleton
class GetGlobalTransferUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    sealed class Result {
        data class OnTransferStart(val transfer: MegaTransfer?) : Result()
        data class OnTransferFinish(val transfer: MegaTransfer?, val error: MegaError) : Result()
        data class OnTransferUpdate(val transfer: MegaTransfer?) : Result()
        data class OnTransferTemporaryError(val transfer: MegaTransfer?, val error: MegaError) : Result()
        data class OnTransferData(val transfer: MegaTransfer?, val buffer: ByteArray?) : Result()
    }

    fun get(): Flowable<Result> =
        Flowable.create({ emitter ->
            val listener = OptionalMegaTransferListenerInterface(
                onTransferStart = { transfer ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnTransferStart(transfer))
                    }
                },
                onTransferFinish = { transfer, error ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnTransferFinish(transfer, error))
                    }
                },
                onTransferUpdate = { transfer ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnTransferUpdate(transfer))
                    }
                },
                onTransferTemporaryError = { transfer, error ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnTransferTemporaryError(transfer, error))
                    }
                },
                onTransferData = { transfer, buffer ->
                    if (!emitter.isCancelled) {
                        emitter.onNext(Result.OnTransferData(transfer, buffer))
                    }
                }
            )

            megaApi.addTransferListener(listener)

            emitter.setCancellable {
                megaApi.removeTransferListener(listener)
            }
        }, BackpressureStrategy.BUFFER)
}

package mega.privacy.android.data.extensions

import kotlinx.coroutines.channels.ProducerScope
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import timber.log.Timber

internal fun <T> ProducerScope<T>.transferListener(
    methodName: String,
    block: (request: MegaTransfer) -> T,
) = OptionalMegaTransferListenerInterface(
    onTransferStart = { transfer ->
        this.trySend(block(transfer))
    },
    onTransferFinish = { transfer, error ->
        when (error.errorCode) {
            MegaError.API_OK, MegaError.API_EEXIST, MegaError.API_ENOENT -> {
                this.trySend(block(transfer))
            }

            else -> {
                // log the error code when calling SDK api, it helps us easy to find the cause
                Timber.e("Calling $methodName failed with error code ${error.errorCode}")
                this.close(error.toException(methodName))
            }
        }
        this.close()
    },
    onTransferUpdate = { transfer ->
        this.trySend(block(transfer))
    },
    onTransferTemporaryError = { _, error ->
        if (error.errorCode == MegaError.API_EOVERQUOTA) {
            this.close(error.toException(methodName))
        }
    }
)
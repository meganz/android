package mega.privacy.android.app.usecase.call

import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.constants.EventConstants
import javax.inject.Inject

/**
 * Main use case to get changes in call status
 *
 * @property megaChatApi   Mega Chat API needed to get call information.
 */
class GetCallStatusChangesUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
) {

    /**
     * Method to get call status
     */
    fun getCallStatus(): Flowable<Int> =
        Flowable.create({ emitter ->
            val callStatusObserver = Observer<MegaChatCall> { call ->
                emitter.onNext(call.status)
            }

            LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                .observeForever(callStatusObserver)

            emitter.setCancellable {
                LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                    .removeObserver(callStatusObserver)
            }
        }, BackpressureStrategy.LATEST)
}
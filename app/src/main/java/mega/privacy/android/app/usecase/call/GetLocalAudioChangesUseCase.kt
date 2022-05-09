package mega.privacy.android.app.usecase.call

import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import mega.privacy.android.app.constants.EventConstants

import nz.mega.sdk.*
import javax.inject.Inject

/**
 * Main use case to get changes in local audio
 *
 * @property megaChatApi   Mega Chat API needed to get call information.
 */
class GetLocalAudioChangesUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid
) {

    /**
     * Method to get local audio changes
     *
     * @return Flowable containing True, if audio is enabled. False, if audio is disabled.
     */
    fun get(): Flowable<MegaChatCall> =
        Flowable.create({ emitter ->
            val localAVFlagsObserver = Observer<MegaChatCall> { call ->
                emitter.onNext(call)
            }

            LiveEventBus.get(EventConstants.EVENT_LOCAL_AVFLAGS_CHANGE, MegaChatCall::class.java)
                .observeForever(localAVFlagsObserver)

            emitter.setCancellable {
                LiveEventBus.get(
                    EventConstants.EVENT_LOCAL_AVFLAGS_CHANGE,
                    MegaChatCall::class.java
                )
                    .removeObserver(localAVFlagsObserver)
            }
        }, BackpressureStrategy.LATEST)
}
package mega.privacy.android.app.usecase.call

import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import mega.privacy.android.app.constants.EventConstants
import javax.inject.Inject

/**
 * Main use case to get changes in network quality
 *
 * @property megaChatApi   Mega Chat API needed to get call information.
 */
class GetNetworkChangesUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
) {

    /**
     * Enum defining the type of network quality.
     *
     */
    enum class NetworkQuality {
        /**
         * Network quality is bad
         */
        NETWORK_QUALITY_BAD,

        /**
         * Network quality is good
         */
        NETWORK_QUALITY_GOOD
    }

    /**
     * Method to get network quality changes
     *
     * @return Flowable with NETWORK_QUALITY_BAD, if network quality is bad. NETWORK_QUALITY_GOOD, if network quality is good.
     */
    fun get(chatId: Long): Flowable<NetworkQuality> =
        Flowable.create({ emitter ->
            val currentCall = megaChatApi.getChatCall(chatId)
            emitter.checkCallNetworkQuality(currentCall)
            val localNetworkQualityObserver = Observer<MegaChatCall> { call ->
                if (chatId == call.chatid) {
                    emitter.checkCallNetworkQuality(call)
                }
            }

            LiveEventBus.get(EventConstants.EVENT_LOCAL_NETWORK_QUALITY_CHANGE,
                MegaChatCall::class.java)
                .observeForever(localNetworkQualityObserver)

            emitter.setCancellable {
                LiveEventBus.get(
                    EventConstants.EVENT_LOCAL_NETWORK_QUALITY_CHANGE,
                    MegaChatCall::class.java
                )
                    .removeObserver(localNetworkQualityObserver)
            }
        }, BackpressureStrategy.LATEST)


    private fun FlowableEmitter<NetworkQuality>.checkCallNetworkQuality(call: MegaChatCall) {
        when (call.networkQuality) {
            MegaChatCall.NETWORK_QUALITY_BAD -> this.onNext(NetworkQuality.NETWORK_QUALITY_BAD)
            MegaChatCall.NETWORK_QUALITY_GOOD -> this.onNext(NetworkQuality.NETWORK_QUALITY_GOOD)
        }
    }
}
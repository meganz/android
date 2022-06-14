package mega.privacy.android.app.usecase.call

import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatCall.TERM_CODE_ERROR
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
     * Enum defining the type of network quality.
     *
     */
    enum class ReconnectingStatusTypes {
        /**
         * Call initiating
         */
        CALL_INITIATING,

        /**
         * Call in progress
         */
        CALL_IN_PROGRESS,

        /**
         * Call reconnecting
         */
        CALL_RECONNECTING
    }

    private var reconnectingStatusList = hashMapOf<Long, ReconnectingStatusTypes>()

    /**
     * Method to check if the call is reconnecting or not
     */
    fun getReconnectingStatus(): Flowable<Boolean> =
        Flowable.create({ emitter ->
            val callStatusObserver = Observer<MegaChatCall> { call ->
                emitter.checkReconnectingStatus(call.chatid, call.status)
            }

            LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                .observeForever(callStatusObserver)

            emitter.setCancellable {
                LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                    .removeObserver(callStatusObserver)
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Method to know if the call has been destroyed because it could not be reconnected.
     */
    fun callCannotBeRecovered(): Flowable<Boolean> =
        Flowable.create({ emitter ->
            val callStatusObserver = Observer<MegaChatCall> { call ->
                if (call.status == MegaChatCall.CALL_STATUS_DESTROYED) {
                    if(call.termCode == TERM_CODE_ERROR && !Util.isOnline(MegaApplication.getInstance().applicationContext)) {
                        emitter.onNext(true)
                    }
                }
            }

            LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                .observeForever(callStatusObserver)

            emitter.setCancellable {
                LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                    .removeObserver(callStatusObserver)
            }
        }, BackpressureStrategy.LATEST)

    /**
     * Method to get the reconnecting status changes
     *
     * @return Flowable with true, if it's in reconnecting status. False, if it's in progress status.
     */
    fun getReconnectingStatus(chatId: Long): Flowable<Boolean> =
        Flowable.create({ emitter ->
            megaChatApi.getChatCall(chatId)?.let { currentCall ->
                emitter.checkReconnectingStatus(chatId, currentCall.status)
            }

            val callStatusObserver = Observer<MegaChatCall> { call ->
                if (chatId == call.chatid) {
                    emitter.checkReconnectingStatus(chatId, call.status)
                }
            }

            LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                .observeForever(callStatusObserver)

            emitter.setCancellable {
                LiveEventBus.get(EventConstants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
                    .removeObserver(callStatusObserver)
            }
        }, BackpressureStrategy.LATEST)


    private fun FlowableEmitter<Boolean>.checkReconnectingStatus(chatId: Long, currentStatus: Int) {
        if (!reconnectingStatusList.contains(chatId)) {
            reconnectingStatusList[chatId] = ReconnectingStatusTypes.CALL_INITIATING
        }

        val reconnectingStatus: ReconnectingStatusTypes? = reconnectingStatusList[chatId]
        when (currentStatus) {
            MegaChatCall.CALL_STATUS_CONNECTING -> {
                if (reconnectingStatus == ReconnectingStatusTypes.CALL_IN_PROGRESS) {
                    this.onNext(true)
                    reconnectingStatusList[chatId] = ReconnectingStatusTypes.CALL_RECONNECTING
                }
                return
            }
            MegaChatCall.CALL_STATUS_JOINING, MegaChatCall.CALL_STATUS_IN_PROGRESS -> {
                if (reconnectingStatus == ReconnectingStatusTypes.CALL_RECONNECTING) {
                    this.onNext(false)
                }
                reconnectingStatusList[chatId] = ReconnectingStatusTypes.CALL_IN_PROGRESS
                return
            }
            else -> {
                reconnectingStatusList.remove(chatId)
                return
            }
        }
    }
}
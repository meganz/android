package mega.privacy.android.app.usecase.call

import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.constants.EventConstants

import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall.*
import java.util.ArrayList
import javax.inject.Inject

/**
 * Main use case to get a Mega Chat Call and get information about existing calls
 *
 * @property megaChatApi   Mega Chat API needed to get call information.
 */
class GetCallUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid
) {

    fun isCurrentCallInProgress(chatId: Long): Flowable<Boolean> =
        Flowable.create({ emitter ->
            megaChatApi.getChatCall(chatId)?.let { call ->
                when (call.status) {
                    CALL_STATUS_IN_PROGRESS -> {
                        emitter.onNext(true)
                    }
                    else -> emitter.onNext(false)
                }
            }

            val callStatusObserver = androidx.lifecycle.Observer<MegaChatCall> { call ->
                if(call.chatid == chatId){
                    when (call.status) {
                        CALL_STATUS_IN_PROGRESS -> {
                            emitter.onNext(true)
                        }
                        else -> emitter.onNext(false)
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
     * Method to get if there is currently a call in progress, joining or connecting
     *
     * @return              Flowable containing True, if there is a ongoing call. False, if not.
     */
    fun isThereAnOngoingCall(): Flowable<Boolean> =
        Flowable.create({ emitter ->
            val callStatusObserver = androidx.lifecycle.Observer<MegaChatCall> { call ->
                when (call.status) {
                    CALL_STATUS_USER_NO_PRESENT, CALL_STATUS_DESTROYED -> {
                        val result: Boolean = getCallInProgress() != null
                        emitter.onNext(result)
                    }
                    CALL_STATUS_CONNECTING, CALL_STATUS_JOINING, CALL_STATUS_IN_PROGRESS -> {
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
     * Method to get if the call associated a chat ID is in progress or in other status
     *
     * @return Flowable containing True, if there call is in progress. False, if the call is in another state.
     */
    fun isThereAnInProgressCall(chatId: Long): Flowable<Boolean> =
        Flowable.create({ emitter ->
            val callStatusObserver = androidx.lifecycle.Observer<MegaChatCall> { call ->
                if (chatId == call.chatid) {
                    when (call.status) {
                        CALL_STATUS_IN_PROGRESS -> emitter.onNext(true)
                        else -> emitter.onNext(false)
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
     * Method to get the call in progress
     *
     * @return The ongoing call
     */
    private fun getCallInProgress(): MegaChatCall? =
        getCallsInProgressAndOnHold().firstOrNull { call -> !call.isOnHold }

    /**
     * Method to get the all calls with status connecting, joining and in progress
     *
     * @return List of ongoing calls
     */
    private fun getCallsInProgressAndOnHold(): ArrayList<MegaChatCall> {
        val listCalls = ArrayList<MegaChatCall>()

        megaChatApi.chatCalls?.let {
            for (i in 0 until it.size()) {
                val chatId = it[i]
                megaChatApi.getChatCall(chatId)?.let { call ->
                    if (call.status == CALL_STATUS_CONNECTING ||
                        call.status == CALL_STATUS_JOINING ||
                        call.status == CALL_STATUS_IN_PROGRESS
                    ) {
                        listCalls.add(call)
                    }
                }
            }
        }

        return listCalls
    }

    /**
     * Checks exists another call in progress
     *
     * @param chatId Chat ID of the current call
     * @return Chat ID of the another call or -1 if no exists
     */
    fun getAnotherCallInProgress(chatId: Long): Single<Long> =
        Single.create { emitter ->
            val callsList: ArrayList<MegaChatCall> = getCallsInProgressAndOnHold()

            for (call in callsList) {
                if (call.chatid != chatId) {
                    emitter.onSuccess(call.chatid)
                    return@create
                }
            }

            emitter.onSuccess(MEGACHAT_INVALID_HANDLE)
            return@create
        }
}

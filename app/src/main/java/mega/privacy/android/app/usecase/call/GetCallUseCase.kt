package mega.privacy.android.app.usecase.call


import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.ArrayList
import javax.inject.Inject

/**
 * Main use case to get a Mega Chat Call.
 *
 * @property megaChatApi    Mega Chat API needed to get call information.
 */
class GetCallUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid
) {

    fun getCurrentCallInProgress(): Single<MegaChatCall>? =
        Single.fromCallable {
            getCallInProgress()
        }

    fun getCurrentCallUserNoPresent(): Single<MegaChatCall>? =
        Single.fromCallable {
            getCallsUserNoParticipating()
        }


    private fun getCallInProgress(): MegaChatCall? {
        getCallsInProgress().forEach {
            if (it != MEGACHAT_INVALID_HANDLE) {
                megaChatApi.getChatCall(it)?.let { call ->
                    if (!call.isOnHold) {
                        return call
                    }
                }

            }
        }

        return null
    }

    private fun getCallsUserNoParticipating(): MegaChatCall? {
        getCallWithStatus(MegaChatCall.CALL_STATUS_USER_NO_PRESENT).let {
           if(it[0] != MEGACHAT_INVALID_HANDLE){
               megaChatApi.getChatCall(it[0])?.let { call ->
                   return call
               }
           }
        }

        return null
    }



    private fun getCallsInProgress(): ArrayList<Long> {
        val listCalls = ArrayList<Long>()

        getCallWithStatus(MegaChatCall.CALL_STATUS_CONNECTING).let {
            listCalls.addAll(it)
        }

        getCallWithStatus(MegaChatCall.CALL_STATUS_JOINING).let {
            listCalls.addAll(it)
        }

        getCallWithStatus(MegaChatCall.CALL_STATUS_IN_PROGRESS).let {
            listCalls.addAll(it)
        }

        return listCalls
    }

    private fun getCallWithStatus(status: Int): ArrayList<Long> {
        val listCalls = ArrayList<Long>()
        megaChatApi.getChatCalls(status)?.let {
            for (i in 0 until it.size()) {
                listCalls.add(it[i])
            }
        }

        return listCalls
    }

}

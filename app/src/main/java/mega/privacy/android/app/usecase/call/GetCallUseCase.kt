package mega.privacy.android.app.usecase.call

import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCall.CALL_STATUS_CONNECTING
import nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS
import nz.mega.sdk.MegaChatCall.CALL_STATUS_JOINING
import javax.inject.Inject

/**
 * Main use case to get a Mega Chat Call and get information about existing calls
 *
 * @property getChatChangesUseCase      Use case required to get chat request updates
 * @property megaChatApi   Mega Chat API needed to get call information.
 */
    class GetCallUseCase @Inject constructor(
    private val getChatChangesUseCase: GetChatChangesUseCase,
    private val megaChatApi: MegaChatApiAndroid,
) {
    /**
     * Method to get the all calls with status connecting, joining and in progress
     *
     * @return List of ongoing calls
     */
    fun getCallsInProgressAndOnHold(): ArrayList<MegaChatCall> {
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
}

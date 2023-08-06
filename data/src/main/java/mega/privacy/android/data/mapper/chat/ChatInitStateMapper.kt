package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatInitState
import nz.mega.sdk.MegaChatApi
import javax.inject.Inject

/**
 * Mapper to map chat init state to [ChatInitState]
 */
class ChatInitStateMapper @Inject constructor() {
    /**
     * Invoke
     * @param state as Int
     * @return chat init state as [ChatInitState]
     */
    operator fun invoke(state: Int): ChatInitState = when (state) {
        MegaChatApi.INIT_ERROR -> ChatInitState.ERROR
        MegaChatApi.INIT_NOT_DONE -> ChatInitState.NOT_DONE
        MegaChatApi.INIT_WAITING_NEW_SESSION -> ChatInitState.WAITING_NEW_SESSION
        MegaChatApi.INIT_OFFLINE_SESSION -> ChatInitState.OFFLINE
        MegaChatApi.INIT_ONLINE_SESSION -> ChatInitState.ONLINE
        MegaChatApi.INIT_ANONYMOUS -> ChatInitState.ANONYMOUS
        MegaChatApi.INIT_TERMINATED -> ChatInitState.TERMINATED
        MegaChatApi.INIT_NO_CACHE -> ChatInitState.NO_CACHE
        else -> ChatInitState.INVALID
    }
}
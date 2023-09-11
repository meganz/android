package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for getting the in progress call
 */
class GetChatCallInProgress @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @return                  [ChatCall]
     */
    suspend operator fun invoke(): ChatCall? = with(callRepository) {
        getCall(getCallHandleList(ChatCallStatus.Initial))?.let {
            return@with it
        }

        getCall(getCallHandleList(ChatCallStatus.Connecting))?.let {
            return@with it
        }

        getCall(getCallHandleList(ChatCallStatus.Joining))?.let {
            return@with it
        }

        getCall(getCallHandleList(ChatCallStatus.InProgress))?.let {
            return@with it
        }

        return@with null
    }

    private suspend fun getCall(calls: List<Long>): ChatCall? {
        calls.forEach {
            callRepository.getChatCall(it)?.let { call ->
                if (!call.isOnHold) {
                    return call
                }
            }
        }

        return null
    }
}
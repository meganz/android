package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus.Connecting
import mega.privacy.android.domain.entity.meeting.ChatCallStatus.InProgress
import mega.privacy.android.domain.entity.meeting.ChatCallStatus.Initial
import mega.privacy.android.domain.entity.meeting.ChatCallStatus.Joining
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import javax.inject.Inject

/**
 * A use case to determines whether there are ongoing video calls.
 *
 * @property getCallHandleListUseCase Use case to get call handle list use case.
 * @property getChatCallUseCase Use case to get chat call.
 * @property defaultDispatcher A [CoroutineDispatcher] to execute the process.
 */
class AreThereOngoingVideoCallsUseCase @Inject constructor(
    private val getCallHandleListUseCase: GetCallHandleListUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Invocation method.
     *
     * @return Boolean. Whether there are ongoing video calls.
     */
    suspend operator fun invoke(): Boolean = withContext(defaultDispatcher) {
        val initialStateCallIDs = getCallHandleListUseCase(Initial)
        val connectingStateCallIDs = getCallHandleListUseCase(Connecting)
        val inProgressStateCallIDs = getCallHandleListUseCase(InProgress)
        val joiningStateCallIDs = getCallHandleListUseCase(Joining)
        val ongoingCallIDs =
            initialStateCallIDs + connectingStateCallIDs + inProgressStateCallIDs + joiningStateCallIDs

        var chatCall: ChatCall? = null
        for (id in ongoingCallIDs) {
            val call = getChatCallUseCase(id)
            if (call?.isOnHold?.not() == true) {
                chatCall = call
                break
            }
        }

        chatCall?.hasLocalVideo == true
    }
}

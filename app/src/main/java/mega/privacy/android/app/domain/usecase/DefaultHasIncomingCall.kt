package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.*
import mega.privacy.android.app.domain.entity.CallOnHoldChange
import mega.privacy.android.app.domain.entity.CallStatusChange
import mega.privacy.android.app.domain.repository.ChatRepository
import javax.inject.Inject

class DefaultHasIncomingCall @Inject constructor(
    private val chatRepository: ChatRepository,
    private val isOnCall: IsOnCall,
) : HasIncomingCall {
    override fun invoke(): Flow<Boolean> {
        return flow {
            emit(hasUnhandledCalls())
            emitAll(chatRepository.monitorCallStateChanges()
                .filter {
                    it is CallStatusChange || it is CallOnHoldChange
                }
                .map {
                    hasUnhandledCalls()
                })
        }
    }

    private suspend fun hasUnhandledCalls(): Boolean {
        val calls = chatRepository.getNumberOfCalls()
        return hasMoreThanOneCall(calls) || isNotOnOnlyCall(calls)
    }

    private suspend fun isNotOnOnlyCall(calls: Int): Boolean = calls == 1 && !isOnCall()

    private fun hasMoreThanOneCall(calls: Int): Boolean = calls > 1
}

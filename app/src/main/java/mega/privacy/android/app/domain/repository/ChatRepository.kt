package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.CallStatus
import mega.privacy.android.app.domain.entity.CallStateChange

interface ChatRepository {
    suspend fun getUnreadNotificationCount(): Int
    fun getUnreadNotificationCountChanges(): Flow<Int>
    suspend fun getNumberOfCalls(): Int
    fun monitorCallStateChanges(): Flow<CallStateChange>
    suspend fun getCallCountByState(callStatus: CallStatus): Long
}

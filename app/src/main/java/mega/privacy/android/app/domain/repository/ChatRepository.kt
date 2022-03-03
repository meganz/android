package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun notifyChatLogout(): Flow<Boolean>
}
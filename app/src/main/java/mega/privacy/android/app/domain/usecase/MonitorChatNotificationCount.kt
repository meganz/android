package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

interface MonitorChatNotificationCount {
    operator fun invoke(): Flow<Int>
}
package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.usecase.account.GetNotificationCountUseCase
import javax.inject.Inject

class NotifyNotificationCountChangeUseCase @Inject constructor(
    private val getNotificationCountUseCase: GetNotificationCountUseCase,
    private val broadcastHomeBadgeCountUseCase: BroadcastHomeBadgeCountUseCase,
) {
    suspend operator fun invoke() {
        val notificationCount = runCatching {
            getNotificationCountUseCase(
                withChatNotifications = false,
            )
        }.getOrNull() ?: 0
        broadcastHomeBadgeCountUseCase(notificationCount)
    }

}
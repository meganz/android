package mega.privacy.android.domain.usecase.notifications

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.qualifier.SystemTime
import mega.privacy.android.domain.repository.PermissionRepository
import javax.inject.Inject

class ShouldShowNotificationReminderUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository,
    @SystemTime private val currentTimeProvider: () -> Long
) {
    suspend operator fun invoke(): Boolean {
        val timestamp = permissionRepository
            .monitorNotificationPermissionShownTimestamp()
            .firstOrNull()

        return timestamp != null && isMoreThan2DaysAgo(timestamp)
    }

    private fun isMoreThan2DaysAgo(timestamp: Long): Boolean {
        val twoDaysInMillis = 2 * 24 * 60 * 60 * 1000L
        return (currentTimeProvider() - timestamp) > twoDaysInMillis
    }
}
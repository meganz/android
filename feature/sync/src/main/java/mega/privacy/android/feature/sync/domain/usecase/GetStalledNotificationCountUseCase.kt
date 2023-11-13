package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.domain.usecase.notifications.GetFeatureNotificationCountUseCase
import javax.inject.Inject

/**
 * Get stalled notification use case
 */
class GetStalledNotificationCountUseCase @Inject constructor(
    private val getSyncStalledIssuesUseCase: GetSyncStalledIssuesUseCase,
) : GetFeatureNotificationCountUseCase {

    override suspend fun invoke(): Int = getSyncStalledIssuesUseCase().size
}
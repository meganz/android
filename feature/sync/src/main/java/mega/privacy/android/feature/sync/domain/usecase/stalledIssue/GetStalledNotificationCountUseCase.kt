package mega.privacy.android.feature.sync.domain.usecase.stalledIssue

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.usecase.notifications.GetFeatureNotificationCountUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.shared.sync.domain.IsSyncFeatureEnabledUseCase
import javax.inject.Inject

/**
 * Get stalled notification use case
 */
class GetStalledNotificationCountUseCase @Inject constructor(
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val isSyncFeatureEnabledUseCase: IsSyncFeatureEnabledUseCase,
) : GetFeatureNotificationCountUseCase {

    override suspend fun invoke(): Int =
        if (isSyncFeatureEnabledUseCase()) {
            monitorSyncStalledIssuesUseCase().first().size
        } else {
            0
        }
}
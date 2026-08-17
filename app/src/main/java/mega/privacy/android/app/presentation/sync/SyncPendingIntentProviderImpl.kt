package mega.privacy.android.app.presentation.sync

import android.app.PendingIntent
import android.content.Context
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.ui.notification.SyncPendingIntentProvider
import mega.privacy.android.navigation.destination.SyncListNavKey
import mega.privacy.android.navigation.destination.SyncTab
import javax.inject.Inject

class SyncPendingIntentProviderImpl @Inject constructor() : SyncPendingIntentProvider {
    override fun invoke(
        context: Context,
        syncNotificationMessage: SyncNotificationMessage,
    ): PendingIntent {
        val initialTab = when (syncNotificationMessage.syncNotificationType) {
            SyncNotificationType.STALLED_ISSUE,
            SyncNotificationType.CROSS_DEVICE_CONFLICT -> SyncTab.STALLED_ISSUES
            else -> SyncTab.FOLDERS
        }
        return MegaActivity.getPendingIntentWithExtraDestination(
            context = context,
            navKey = SyncListNavKey(initialTab = initialTab)
        )
    }
}

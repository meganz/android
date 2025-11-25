package mega.privacy.android.app.presentation.sync

import android.app.PendingIntent
import android.content.Context
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.ui.notification.SyncPendingIntentProvider
import mega.privacy.android.navigation.destination.SyncListNavKey
import javax.inject.Inject

class SyncPendingIntentProviderImpl @Inject constructor() : SyncPendingIntentProvider {
    override fun invoke(
        context: Context,
        syncNotificationMessage: SyncNotificationMessage,
    ): PendingIntent {
        return MegaActivity.getPendingIntentWithExtraDestination(
            context = context,
            navKey = SyncListNavKey
        )
    }
}

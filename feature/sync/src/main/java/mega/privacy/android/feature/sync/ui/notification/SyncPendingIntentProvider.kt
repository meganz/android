package mega.privacy.android.feature.sync.ui.notification

import android.app.PendingIntent
import android.content.Context
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage

interface SyncPendingIntentProvider {
    operator fun invoke(
        context: Context,
        syncNotificationMessage: SyncNotificationMessage,
    ): PendingIntent
}

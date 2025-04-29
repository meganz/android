package mega.privacy.android.app.presentation.transfers.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.shared.resources.R
import javax.inject.Inject

/**
 * Gets the action text for ActionGroup finish Notifications
 */
class ActionGroupFinishNotificationActionTextMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resources get() = context.resources

    /**
     * Invoke
     */
    operator fun invoke(
        isLoggedIn: Boolean,
        isPreviewDownload: Boolean,
        isOfflineDownload: Boolean,
        actionGroup: ActiveTransferTotals.ActionGroup,
    ): String? = when {
        actionGroup.completedFiles == 0 && actionGroup.alreadyTransferred == 0 -> null
        isPreviewDownload -> resources.getString(R.string.transfers_notification_preview_action)
        isLoggedIn || isOfflineDownload.not() -> resources.getString(R.string.transfers_notification_location_action)
        else -> null
    }
}
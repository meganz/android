package mega.privacy.android.app.presentation.transfers.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsContentUriUseCase
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Gets the location for ActionGroup Notifications
 */
class ActionGroupNotificationDestinationMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val isContentUriUseCase: IsContentUriUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(
        isPreviewDownload: Boolean,
        isOfflineDownload: Boolean,
        isDownload: Boolean,
        actionGroup: ActiveTransferTotals.ActionGroup,
    ) = when {
        isOfflineDownload -> context.getString(R.string.section_saved_for_offline_new)
        isPreviewDownload -> null
        isDownload -> runCatching {
            if (isContentUriUseCase(actionGroup.destination)) {
                getPathByDocumentContentUriUseCase(actionGroup.destination)
            } else {
                actionGroup.destination
            }
        }.getOrNull() ?: actionGroup.destination

        else -> actionGroup.destination
    }?.let { destination ->
        context.getString(
            sharedR.string.transfers_notification_location_content,
            destination,
        )
    }
}
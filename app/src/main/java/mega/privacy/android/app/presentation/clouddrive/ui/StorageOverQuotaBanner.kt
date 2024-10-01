package mega.privacy.android.app.presentation.clouddrive.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.clouddrive.model.StorageOverQuotaCapacity
import mega.privacy.android.shared.original.core.ui.controls.banners.InlineErrorBanner
import mega.privacy.android.shared.original.core.ui.controls.banners.InlineWarningBanner
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerUpgradeButtonPressedEvent

/**
 * Composable for the Storage Over Quota Banner
 * @param storageCapacity the storage capacity
 * @param onStorageFullWarningDismiss the callback when the storage full warning is dismissed
 * @param onUpgradeClicked the callback when the upgrade is clicked
 */
@Composable
fun StorageOverQuotaBanner(
    storageCapacity: StorageOverQuotaCapacity,
    onStorageFullWarningDismiss: () -> Unit,
    onUpgradeClicked: () -> Unit,
) {

    val storageFullActionString =
        stringResource(id = R.string.account_storage_over_quota_inline_error_banner_upgrade_link)

    if (storageCapacity == StorageOverQuotaCapacity.FULL) {
        Analytics.tracker.trackEvent(FullStorageOverQuotaBannerDisplayedEvent)
        InlineErrorBanner(
            title = stringResource(id = R.string.account_storage_over_quota_inline_error_banner_title),
            message = stringResource(id = R.string.account_storage_over_quota_inline_error_banner_message),
            actionButtonText = storageFullActionString,
            onActionButtonClick = {
                Analytics.tracker.trackEvent(FullStorageOverQuotaBannerUpgradeButtonPressedEvent)
                onUpgradeClicked()
            })
    } else {
        Analytics.tracker.trackEvent(AlmostFullStorageOverQuotaBannerDisplayedEvent)
        InlineWarningBanner(
            title = stringResource(id = R.string.account_storage_over_quota_inline_warning_banner_title),
            message = stringResource(id = R.string.account_storage_over_quota_inline_warning_banner_message),
            actionButtonText = storageFullActionString,
            onActionButtonClick = {
                Analytics.tracker.trackEvent(
                    AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent
                )
                onUpgradeClicked()
            },
            onCloseClick = { onStorageFullWarningDismiss() }
        )
    }
}
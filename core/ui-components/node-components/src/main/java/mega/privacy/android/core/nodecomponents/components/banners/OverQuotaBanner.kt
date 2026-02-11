package mega.privacy.android.core.nodecomponents.components.banners

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.android.core.ui.components.banner.TopErrorBanner
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.sharedcomponents.coroutine.LaunchedOnceEffect
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerUpgradeButtonPressedEvent

// Test tags for UI testing
const val STORAGE_BANNER_M3_ROOT_TEST_TAG = "storage_over_quota_banner_m3:root"

/**
 * Storage Over Quota Banner
 *
 * @param overQuotaStatus
 * @param onUpgradeClicked the callback when the upgrade is clicked
 * @param modifier optional modifier for the banner
 */
@Composable
fun OverQuotaErrorBanner(
    overQuotaStatus: OverQuotaStatus,
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (overQuotaStatus.severity == OverQuotaIssue.Severity.Error) {
        LaunchedOnceEffect(overQuotaStatus.storage.severity) {
            if (overQuotaStatus.storage.severity == OverQuotaIssue.Severity.Error)
                Analytics.tracker.trackEvent(FullStorageOverQuotaBannerDisplayedEvent)
        }
        val title: String
        val body: String?
        when {
            overQuotaStatus.hasTransferIssue && overQuotaStatus.hasStorageIssue -> {
                title =
                    stringResource(id = R.string.transfers_storage_and_transfer_quota_banner_title)
                body = null
            }

            overQuotaStatus.hasStorageIssue -> {
                title =
                    stringResource(id = R.string.account_storage_over_quota_inline_error_banner_title)
                body =
                    stringResource(id = R.string.account_storage_over_quota_inline_error_banner_message)
            }

            else -> {
                title = stringResource(id = R.string.transfers_transfer_quota_banner_title)
                body = null
            }
        }

        TopErrorBanner(
            modifier = modifier.testTag(STORAGE_BANNER_M3_ROOT_TEST_TAG),
            title = title,
            body = body,
            showCancelButton = false,
            forceRiceTopAppBar = true,
            actionButtonText = stringResource(id = R.string.account_storage_over_quota_inline_error_banner_upgrade_link),
            onActionButtonClick = {
                Analytics.tracker.trackEvent(FullStorageOverQuotaBannerUpgradeButtonPressedEvent)
                onUpgradeClicked()
            },
        )
    }
}

/**
 * Storage Over Quota Banner
 *
 * @param overQuotaStatus
 * @param onDismissed the callback when the warning is dismissed
 * @param onUpgradeClicked the callback when the upgrade is clicked
 * @param modifier optional modifier for the banner
 */
@Composable
fun OverQuotaWarningBanner(
    overQuotaStatus: OverQuotaStatus,
    onDismissed: () -> Unit,
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (overQuotaStatus.severity == OverQuotaIssue.Severity.Warning) {
        LaunchedOnceEffect(overQuotaStatus.storage.severity) {
            if (overQuotaStatus.storage.severity == OverQuotaIssue.Severity.Warning)
                Analytics.tracker.trackEvent(AlmostFullStorageOverQuotaBannerDisplayedEvent)
        }
        val title: String
        val body: String?
        when {
            overQuotaStatus.hasTransferIssue && overQuotaStatus.hasStorageIssue -> {
                title =
                    stringResource(id = R.string.transfers_storage_and_transfer_quota_banner_title)
                body = null
            }

            overQuotaStatus.hasStorageIssue -> {
                title =
                    stringResource(id = R.string.account_storage_over_quota_inline_warning_banner_title)
                body =
                    stringResource(id = R.string.account_storage_over_quota_inline_warning_banner_message)
            }

            else -> {
                title = stringResource(id = R.string.transfers_transfer_quota_banner_title)
                body = null
            }
        }
        TopWarningBanner(
            modifier = modifier.testTag(STORAGE_BANNER_M3_ROOT_TEST_TAG),
            title = title,
            body = body,
            showCancelButton = true,
            forceRiceTopAppBar = true,
            actionButtonText = stringResource(id = R.string.account_storage_over_quota_inline_error_banner_upgrade_link),
            onActionButtonClick = {
                Analytics.tracker.trackEvent(
                    AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent
                )
                onUpgradeClicked()
            },
            onCancelButtonClick = {
                Analytics.tracker.trackEvent(
                    AlmostFullStorageOverQuotaBannerCloseButtonPressedEvent
                )
                onDismissed()
            },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewStorageOverQuotaBannerError(
    @PreviewParameter(OverQuotaStatusErrorProvider::class) overQuotaStatus: OverQuotaStatus,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        OverQuotaErrorBanner(
            overQuotaStatus = overQuotaStatus,
            onUpgradeClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewStorageOverQuotaBannerWarning(
    @PreviewParameter(OverQuotaStatusWarningProvider::class) overQuotaStatus: OverQuotaStatus,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        OverQuotaWarningBanner(
            overQuotaStatus = overQuotaStatus,
            onDismissed = {},
            onUpgradeClicked = {},
        )
    }
}

class OverQuotaStatusErrorProvider : PreviewParameterProvider<OverQuotaStatus> {
    override val values = listOf(
        OverQuotaStatus(OverQuotaIssue.Storage.Full),
        OverQuotaStatus(OverQuotaIssue.Storage.Full, OverQuotaIssue.Transfer.TransferOverQuota),
        OverQuotaStatus(transfer = OverQuotaIssue.Transfer.TransferOverQuota),
    ).asSequence()
}

class OverQuotaStatusWarningProvider : PreviewParameterProvider<OverQuotaStatus> {
    override val values = listOf(
        OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
        OverQuotaStatus(transfer = OverQuotaIssue.Transfer.TransferOverQuotaFreeUser),
        OverQuotaStatus(
            OverQuotaIssue.Storage.AlmostFull,
            OverQuotaIssue.Transfer.TransferOverQuotaFreeUser,
        ),
    ).asSequence()
}


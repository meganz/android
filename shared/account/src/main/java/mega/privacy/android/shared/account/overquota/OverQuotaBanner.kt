package mega.privacy.android.shared.account.overquota

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
import mega.android.core.ui.extensions.LaunchedOncePerAppEffect
import mega.android.core.ui.extensions.resetLaunchedOncePerAppEffect
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.AlmostFullStorageAndTransferOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.FullStorageAndTransferOverQuotaErrorBannerDisplayeEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferOverQuotaErrorBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.TransferOverQuotaWarningBannerDisplayedEvent

// Test tags for UI testing
const val STORAGE_ERROR_BANNER_ROOT_TEST_TAG = "storage_over_quota_banner:root"
const val STORAGE_WARNING_BANNER_ROOT_TEST_TAG = "storage_over_quota_warning_banner:root"

/**
 * Over Quota Banner for both, error or warning
 *
 * @param overQuotaStatus
 * @param onDismissed the callback when the warning is dismissed
 * @param onUpgradeClicked the callback when the upgrade is clicked
 * @param modifier optional modifier for the banner
 * @param isBlockingAware if true, the banner should be shown only if it's blocking transfers (with a simplified message).
 */
@Composable
fun OverQuotaBanner(
    overQuotaStatus: OverQuotaStatus,
    onDismissed: () -> Unit,
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isBlockingAware: Boolean = false,
    forceRiceTopAppBar: Boolean = true,
) {
    if (overQuotaStatus.severity is OverQuotaIssue.Severity.Warning) {
        OverQuotaWarningBanner(
            overQuotaStatus = overQuotaStatus,
            onDismissed = onDismissed,
            onUpgradeClicked = onUpgradeClicked,
            modifier = modifier,
            isBlockingAware = isBlockingAware,
            forceRiceTopAppBar = forceRiceTopAppBar,
        )
    } else if (overQuotaStatus.severity is OverQuotaIssue.Severity.Error) {
        OverQuotaErrorBanner(
            overQuotaStatus = overQuotaStatus,
            onUpgradeClicked = onUpgradeClicked,
            modifier = modifier,
            isBlockingAware = isBlockingAware,
            forceRiceTopAppBar = forceRiceTopAppBar,
        )
    }
}

/**
 * Storage Over Quota Banner
 *
 * @param overQuotaStatus
 * @param onUpgradeClicked the callback when the upgrade is clicked
 * @param modifier optional modifier for the banner
 * @param isBlockingAware if true, the banner should be shown only if it's blocking transfers (with a simplified message).
 */
@Composable
fun OverQuotaErrorBanner(
    overQuotaStatus: OverQuotaStatus,
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isBlockingAware: Boolean = false,
    forceRiceTopAppBar: Boolean = true,
) {
    if (overQuotaStatus.severity == OverQuotaIssue.Severity.Error) {
        val title: String
        val body: String?
        val analyticsEvent: EventIdentifier
        when {
            overQuotaStatus.hasTransferIssue && overQuotaStatus.hasStorageIssue -> {
                title =
                    stringResource(id = R.string.transfers_storage_and_transfer_quota_banner_title)
                body = null
                analyticsEvent = FullStorageAndTransferOverQuotaErrorBannerDisplayeEvent
            }

            overQuotaStatus.hasStorageIssue -> {
                analyticsEvent = FullStorageOverQuotaBannerDisplayedEvent
                title = stringResource(
                    id = if (isBlockingAware) {
                        R.string.transfers_storage_quota_banner_title
                    } else {
                        R.string.account_storage_over_quota_inline_error_banner_title
                    }
                )
                body = if (isBlockingAware) null else {
                    stringResource(id = R.string.account_storage_over_quota_inline_error_banner_message)
                }
            }

            else -> {
                analyticsEvent = TransferOverQuotaErrorBannerDisplayedEvent
                title = stringResource(id = R.string.transfers_transfer_quota_banner_title)
                body = null
            }
        }
        LaunchedOncePerAppEffect(analyticsEvent) {
            Analytics.tracker.trackEvent(analyticsEvent)
        }

        TopErrorBanner(
            modifier = modifier.testTag(STORAGE_ERROR_BANNER_ROOT_TEST_TAG),
            title = title,
            body = body,
            showCancelButton = false,
            forceRiceTopAppBar = forceRiceTopAppBar,
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
 * @param isBlockingAware if true, the banner should be shown only if it's blocking transfers (with a simplified message).
 */
@Composable
fun OverQuotaWarningBanner(
    overQuotaStatus: OverQuotaStatus,
    onDismissed: () -> Unit,
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isBlockingAware: Boolean = false,
    forceRiceTopAppBar: Boolean = true,
) {
    if (overQuotaStatus.severity is OverQuotaIssue.Severity.Warning && (!isBlockingAware || overQuotaStatus.severity is OverQuotaIssue.Severity.Warning.Blocking)) {
        val analyticsEvent: EventIdentifier
        val title: String
        val body: String?
        when {
            overQuotaStatus.hasTransferIssue && overQuotaStatus.hasStorageIssue -> {
                title =
                    stringResource(id = R.string.transfers_storage_and_transfer_quota_banner_title)
                body = null
                analyticsEvent = AlmostFullStorageAndTransferOverQuotaBannerDisplayedEvent
            }

            overQuotaStatus.hasStorageIssue -> {
                title =
                    stringResource(id = R.string.account_storage_over_quota_inline_warning_banner_title)
                body =
                    stringResource(id = R.string.account_storage_over_quota_inline_warning_banner_message)
                analyticsEvent = AlmostFullStorageOverQuotaBannerDisplayedEvent
            }

            else -> {
                title = stringResource(id = R.string.transfers_transfer_quota_banner_title)
                body = null
                analyticsEvent = TransferOverQuotaWarningBannerDisplayedEvent
            }
        }
        LaunchedOncePerAppEffect(analyticsEvent) {
            Analytics.tracker.trackEvent(analyticsEvent)
        }
        TopWarningBanner(
            modifier = modifier.testTag(STORAGE_WARNING_BANNER_ROOT_TEST_TAG),
            title = title,
            body = body,
            showCancelButton = true,
            forceRiceTopAppBar = forceRiceTopAppBar,
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
                resetLaunchedOncePerAppEffect(analyticsEvent)
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

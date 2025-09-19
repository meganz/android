package mega.privacy.android.core.nodecomponents.components.banners

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerUpgradeButtonPressedEvent

// Test tags for UI testing
const val STORAGE_BANNER_M3_ROOT_TEST_TAG = "storage_over_quota_banner_m3:root"
const val STORAGE_BANNER_M3_ICON_TEST_TAG = "storage_over_quota_banner_m3:icon"
const val STORAGE_BANNER_M3_TITLE_TEST_TAG = "storage_over_quota_banner_m3:title"
const val STORAGE_BANNER_M3_MESSAGE_TEST_TAG = "storage_over_quota_banner_m3:message"
const val STORAGE_BANNER_M3_ACTION_TEST_TAG = "storage_over_quota_banner_m3:action_button"
const val STORAGE_BANNER_M3_CLOSE_TEST_TAG = "storage_over_quota_banner_m3:close_button"

/**
 * M3 version of the Storage Over Quota Banner
 *
 * @param storageCapacity the storage capacity state
 * @param onStorageAlmostFullWarningDismiss the callback when the storage almost full warning is dismissed
 * @param onUpgradeClicked the callback when the upgrade is clicked
 * @param modifier optional modifier for the banner
 */
@Composable
fun StorageOverQuotaBannerM3(
    storageCapacity: StorageOverQuotaCapacity,
    onStorageAlmostFullWarningDismiss: () -> Unit,
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val storageActionString =
        stringResource(id = R.string.account_storage_over_quota_inline_error_banner_upgrade_link)

    when (storageCapacity) {
        StorageOverQuotaCapacity.FULL -> {
            Analytics.tracker.trackEvent(FullStorageOverQuotaBannerDisplayedEvent)
            StorageOverQuotaBannerContentM3(
                modifier = modifier,
                title = stringResource(id = R.string.account_storage_over_quota_inline_error_banner_title),
                message = stringResource(id = R.string.account_storage_over_quota_inline_error_banner_message),
                actionButtonText = storageActionString,
                onActionButtonClick = {
                    Analytics.tracker.trackEvent(FullStorageOverQuotaBannerUpgradeButtonPressedEvent)
                    onUpgradeClicked()
                },
                onCloseClick = null,
                isError = true
            )
        }

        StorageOverQuotaCapacity.ALMOST_FULL -> {
            Analytics.tracker.trackEvent(AlmostFullStorageOverQuotaBannerDisplayedEvent)
            StorageOverQuotaBannerContentM3(
                modifier = modifier,
                title = stringResource(id = R.string.account_storage_over_quota_inline_warning_banner_title),
                message = stringResource(id = R.string.account_storage_over_quota_inline_warning_banner_message),
                actionButtonText = storageActionString,
                onActionButtonClick = {
                    Analytics.tracker.trackEvent(
                        AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent
                    )
                    onUpgradeClicked()
                },
                onCloseClick = {
                    Analytics.tracker.trackEvent(
                        AlmostFullStorageOverQuotaBannerCloseButtonPressedEvent
                    )
                    onStorageAlmostFullWarningDismiss()
                },
                isError = false
            )
        }

        StorageOverQuotaCapacity.DEFAULT -> {
            // Don't show banner for default state
        }
    }
}

@Composable
private fun StorageOverQuotaBannerContentM3(
    title: String,
    message: String,
    actionButtonText: String,
    onActionButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCloseClick: (() -> Unit)? = null,
    isError: Boolean = false,
) {
    val backgroundColor = if (isError) {
        DSTokens.colors.notifications.notificationError
    } else {
        DSTokens.colors.notifications.notificationWarning
    }

    val iconColor = if (isError) {
        DSTokens.colors.support.error
    } else {
        DSTokens.colors.support.warning
    }

    val iconRes = if (isError) {
        IconPackR.drawable.ic_warning_icon
    } else {
        IconPackR.drawable.ic_alert_circle_medium_thin_outline
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(STORAGE_BANNER_M3_ROOT_TEST_TAG)
            .background(color = backgroundColor)
            .padding(LocalSpacing.current.x16),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.x8),
            verticalAlignment = Alignment.Top
        ) {
            MegaIcon(
                modifier = Modifier
                    .size(24.dp)
                    .testTag(STORAGE_BANNER_M3_ICON_TEST_TAG),
                painter = painterResource(id = iconRes),
                supportTint = if (isError) SupportColor.Error else SupportColor.Warning,
                contentDescription = null
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp), // Align with icon visually
                verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.x8)
            ) {
                MegaText(
                    modifier = Modifier.testTag(STORAGE_BANNER_M3_TITLE_TEST_TAG),
                    text = title,
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )

                MegaText(
                    modifier = Modifier.testTag(STORAGE_BANNER_M3_MESSAGE_TEST_TAG),
                    text = message,
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.bodySmall
                )

                Box(
                    modifier = Modifier
                        .testTag(STORAGE_BANNER_M3_ACTION_TEST_TAG)
                        .clickable { onActionButtonClick() }
                        .padding(vertical = 4.dp)
                ) {
                    MegaText(
                        text = actionButtonText,
                        textColor = TextColor.Info,
                        style = AppTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                }
            }

            onCloseClick?.let {
                MegaIcon(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onCloseClick() }
                        .testTag(STORAGE_BANNER_M3_CLOSE_TEST_TAG),
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                    tint = IconColor.Primary,
                    contentDescription = null
                )
            } ?: Spacer(modifier = Modifier.size(0.dp))
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewStorageOverQuotaBannerM3Error() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        StorageOverQuotaBannerM3(
            storageCapacity = StorageOverQuotaCapacity.FULL,
            onStorageAlmostFullWarningDismiss = {},
            onUpgradeClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewStorageOverQuotaBannerM3Warning() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        StorageOverQuotaBannerM3(
            storageCapacity = StorageOverQuotaCapacity.ALMOST_FULL,
            onStorageAlmostFullWarningDismiss = {},
            onUpgradeClicked = {},
        )
    }
}



package mega.privacy.android.feature.photos.presentation.timeline.component

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.SpannedText
import mega.android.core.ui.components.banner.InlineErrorBanner
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.components.divider.StrongDivider
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.presentation.CUStatusUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerUpgradeButtonPressedEvent

@Composable
internal fun CameraUploadsBanner(
    status: CUStatusUiState,
    onEnableCameraUploads: () -> Unit,
    onDismissRequest: (status: CUStatusUiState) -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    onNavigateToCameraUploadsSettings: () -> Unit,
    onNavigateMobileDataSetting: () -> Unit,
    onNavigateUpgradeScreen: () -> Unit,
) {
    when (status) {
        is CUStatusUiState.Disabled -> {
            if (status.shouldNotifyUser) {
                EnableCameraUploadsBanner(
                    onEnableCUClick = onEnableCameraUploads,
                    onDismissRequest = { onDismissRequest(status) }
                )
            }
        }

        CUStatusUiState.Warning.HasLimitedAccess -> {
            CameraUploadsNoFullAccessBanner(
                onClick = onChangeCameraUploadsPermissions,
                onClose = { onDismissRequest(status) },
            )
        }

        CUStatusUiState.Warning.BatteryLevelTooLow -> {
            LowBatteryPausedBanner(onClose = { onDismissRequest(status) })
        }

        CUStatusUiState.Warning.DeviceChargingRequirementNotMet -> {
            DeviceChargingNotMetPausedBanner(
                onOpenSettingsClicked = onNavigateToCameraUploadsSettings
            )
        }

        CUStatusUiState.Warning.NetworkConnectionRequirementNotMet -> {
            NetworkRequirementNotMetPausedBanner(
                onNavigateMobileDataSetting = onNavigateMobileDataSetting
            )
        }

        CUStatusUiState.Warning.AccountStorageOverQuota -> {
            FullStorageBanner(onUpgradeClicked = onNavigateUpgradeScreen)
        }

        else -> Unit
    }
}

@Composable
private fun CameraUploadsNoFullAccessBanner(
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopWarningBanner(
        modifier = modifier.testTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG),
        title = null,
        body = stringResource(sharedR.string.timeline_tab_cu_permission_warning_banner_description),
        actionButtonText = stringResource(sharedR.string.timeline_tab_cu_permission_warning_banner_action),
        showCancelButton = true,
        onActionButtonClick = onClick,
        onCancelButtonClick = onClose
    )
}

@Composable
private fun DeviceChargingNotMetPausedBanner(
    onOpenSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicCameraUploadsBanner(
        modifier = modifier.testTag(TIMELINE_CAMERA_UPLOADS_DEVICE_CHARGING_NOT_MET_BANNER_TEST_TAG),
        statusIcon = R.drawable.ic_cu_status_warning,
        title = stringResource(sharedR.string.camera_update_paused_warning_banner_title),
        description = stringResource(sharedR.string.camera_update_device_charging_not_met_banner_description),
        action = {
            val actionTitle =
                "[A]${stringResource(sharedR.string.camera_update_device_charging_not_met_banner_button_text)}[/A]"
            LinkSpannedText(
                value = actionTitle,
                spanStyles = mapOf(
                    SpanIndicator('A') to SpanStyleWithAnnotation(
                        MegaSpanStyle.LinkColorStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Bold
                            ),
                            LinkColor.Primary
                        ), "A"
                    )
                ),
                baseTextColor = TextColor.Secondary,
                baseStyle = AppTheme.typography.labelMedium,
                onAnnotationClick = { onOpenSettingsClicked() }
            )
        }
    )
}

@Composable
private fun LowBatteryPausedBanner(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopWarningBanner(
        modifier = modifier.testTag(TIMELINE_CAMERA_UPLOADS_LOW_BATTERY_BANNER_TEST_TAG),
        title = null,
        body = stringResource(sharedR.string.timeline_tab_cu_battery_level_warning_banner_description),
        showCancelButton = true,
        onCancelButtonClick = onClose
    )
}

@Composable
private fun NetworkRequirementNotMetPausedBanner(
    onNavigateMobileDataSetting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicCameraUploadsBanner(
        modifier = modifier.testTag(
            TIMELINE_CAMERA_UPLOADS_NETWORK_REQUIREMENT_NOT_MET_PAUSED_BANNER_TEST_TAG
        ),
        statusIcon = R.drawable.ic_cu_status_warning,
        title = stringResource(sharedR.string.camera_update_paused_warning_banner_title),
        description = stringResource(sharedR.string.camera_uploads_notification_content_no_wifi_connection),
        action = {
            val actionTitle =
                "[A]${stringResource(sharedR.string.camera_update_network_requirement_not_met_banner_button_text)}[/A]"
            LinkSpannedText(
                value = actionTitle,
                spanStyles = mapOf(
                    SpanIndicator('A') to SpanStyleWithAnnotation(
                        MegaSpanStyle.LinkColorStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Bold
                            ),
                            LinkColor.Primary
                        ), "A"
                    )
                ),
                baseTextColor = TextColor.Secondary,
                baseStyle = AppTheme.typography.labelMedium,
                onAnnotationClick = { onNavigateMobileDataSetting() }
            )
        }
    )
}

@Composable
private fun FullStorageBanner(
    onUpgradeClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(FullStorageOverQuotaBannerDisplayedEvent)
    }

    InlineErrorBanner(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TIMELINE_CAMERA_UPLOADS_FULL_STORAGE_BANNER_TEST_TAG),
        title = stringResource(id = sharedR.string.account_storage_over_quota_inline_error_banner_title),
        body = stringResource(id = sharedR.string.account_storage_over_quota_inline_error_banner_message),
        actionButtonText = stringResource(id = sharedR.string.account_storage_over_quota_inline_error_banner_upgrade_link),
        showCancelButton = false,
        onActionButtonClick = {
            Analytics.tracker.trackEvent(FullStorageOverQuotaBannerUpgradeButtonPressedEvent)
            onUpgradeClicked()
        }
    )
}

@Composable
private fun EnableCameraUploadsBanner(
    onEnableCUClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicCameraUploadsBanner(
        modifier = modifier.testTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG),
        verticalAlignment = Alignment.Top,
        statusIcon = R.drawable.ic_cu_status,
        title = null,
        description = stringResource(id = sharedR.string.camera_upload_banner_enable_cu_subtitle),
        action = {
            val actionTitle =
                "[A]${stringResource(sharedR.string.camera_backup_permission_enable_button_text)}[/A]"
            LinkSpannedText(
                modifier = Modifier.padding(top = 8.dp),
                value = actionTitle,
                spanStyles = mapOf(
                    SpanIndicator('A') to SpanStyleWithAnnotation(
                        MegaSpanStyle.LinkColorStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            ),
                            LinkColor.Primary
                        ), "A"
                    )
                ),
                baseTextColor = TextColor.Secondary,
                baseStyle = AppTheme.typography.labelLarge,
                onAnnotationClick = { onEnableCUClick() }
            )
        },
        endIcon = {
            MegaIcon(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onDismissRequest)
                    .testTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_DISMISS_ICON_TEST_TAG),
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                contentDescription = "Camera uploads banner end icon",
                tint = IconColor.Primary,
            )
        }
    )
}

@SuppressLint("ComposeUnstableCollections")
@Composable
private fun BasicCameraUploadsBanner(
    @DrawableRes statusIcon: Int,
    title: String?,
    description: String?,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    spanStyles: Map<SpanIndicator, MegaSpanStyle>? = null,
    action: @Composable (() -> Unit)? = null,
    endIcon: @Composable (() -> Unit)? = null,
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = verticalAlignment
        ) {
            MegaIcon(
                modifier = Modifier
                    .padding(end = 15.dp)
                    .size(24.dp),
                painter = painterResource(id = statusIcon),
                contentDescription = "Camera uploads banner status icon",
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    if (title != null && description != null) {
                        Arrangement.spacedBy(5.dp)
                    } else {
                        Arrangement.Top
                    }
            ) {
                title?.let {
                    MegaText(
                        modifier = Modifier,
                        textColor = TextColor.Primary,
                        style = AppTheme.typography.bodyMedium,
                        text = title
                    )
                }

                description?.let {
                    if (spanStyles != null) {
                        SpannedText(
                            value = it,
                            spanStyles = spanStyles,
                            maxLines = Int.MAX_VALUE,
                            baseTextColor = TextColor.Secondary,
                            baseStyle = AppTheme.typography.labelMedium
                        )
                    } else {
                        MegaText(
                            text = it,
                            modifier = Modifier,
                            textColor = TextColor.Secondary,
                            style = AppTheme.typography.bodySmall
                        )
                    }
                }

                action?.let { actionContent ->
                    actionContent()
                }
            }

            endIcon?.let {
                Box(modifier = Modifier.padding(start = 10.dp)) {
                    endIcon()
                }
            }
        }

        StrongDivider(modifier = Modifier.fillMaxWidth())
    }
}

@CombinedThemePreviews
@Composable
private fun EnableCameraUploadsBannerPreview() {
    AndroidThemeForPreviews {
        EnableCameraUploadsBanner(
            onEnableCUClick = {},
            onDismissRequest = {}
        )
    }
}

@Preview
@Composable
private fun PreviewCameraUploadsNoFullAccessBanner() {
    AndroidThemeForPreviews {
        CameraUploadsNoFullAccessBanner(onClick = {}, onClose = {})
    }
}

@Preview
@Composable
private fun PreviewDeviceChargingNotMetPausedBanner() {
    AndroidThemeForPreviews {
        DeviceChargingNotMetPausedBanner(onOpenSettingsClicked = {})
    }
}


@Preview
@Composable
private fun PreviewLowBatteryPausedBanner() {
    AndroidThemeForPreviews {
        LowBatteryPausedBanner(onClose = {})
    }
}

@Preview
@Composable
private fun PreviewNetworkRequirementNotMetBanner() {
    AndroidThemeForPreviews {
        NetworkRequirementNotMetPausedBanner(onNavigateMobileDataSetting = {})
    }
}

@Preview
@Composable
private fun PreviewFullStorageBanner() {
    AndroidThemeForPreviews {
        FullStorageBanner(onUpgradeClicked = {})
    }
}

/**
 * Test tag for camera uploads no full access banner
 */
const val TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG =
    "timeline_camera_uploads_no_full_access_banner_test_tag"

/**
 * Test tag for camera uploads device charging not met banner
 */
const val TIMELINE_CAMERA_UPLOADS_DEVICE_CHARGING_NOT_MET_BANNER_TEST_TAG =
    "timeline_camera_uploads_device_charging_not_met_test_tag"

/**
 * Test tag for camera uploads low battery banner
 */
const val TIMELINE_CAMERA_UPLOADS_LOW_BATTERY_BANNER_TEST_TAG =
    "timeline_camera_uploads_low_battery_test_tag"

/**
 * Test tag for enable camera uploads banner
 */
const val TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG =
    "timeline_enable_camera_uploads_banner_test_tag"

/**
 * Test tag for enable camera uploads banner dismiss icon
 */
const val TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_DISMISS_ICON_TEST_TAG =
    "timeline_enable_camera_uploads_dismiss_icon_test_tag"

/**
 * Test tag for camera uploads allow mobile data banner
 */
const val TIMELINE_CAMERA_UPLOADS_NETWORK_REQUIREMENT_NOT_MET_PAUSED_BANNER_TEST_TAG =
    "timeline_camera_uploads_network_requirement_not_met_paused_banner_test_tag"

/**
 * Test tag for camera uploads full storage banner
 */
const val TIMELINE_CAMERA_UPLOADS_FULL_STORAGE_BANNER_TEST_TAG =
    "timeline_camera_uploads_full_storage_banner_test_tag"

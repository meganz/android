package mega.privacy.android.feature.sync.ui.views

import mega.privacy.android.core.R as coreR
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.banners.WarningBanner
import mega.privacy.android.core.ui.controls.buttons.MegaButtonWithIconAndText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun SyncCard(
    folderPairName: String,
    status: SyncStatus,
    hasStalledIssues: Boolean,
    deviceStoragePath: String,
    megaStoragePath: String,
    method: String,
    expanded: Boolean,
    expandClicked: () -> Unit,
    pauseRunClicked: () -> Unit,
    removeFolderClicked: () -> Unit,
    issuesInfoClicked: () -> Unit,
    isLowBatteryLevel: Boolean,
    @StringRes errorRes: Int?,
    modifier: Modifier = Modifier,
) {

    val roundedCornersShape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = roundedCornersShape,
                spotColor = MaterialTheme.colors.black_white,
                ambientColor = MaterialTheme.colors.black_white,
            )
            .background(
                MaterialTheme.colors.surface, shape = roundedCornersShape
            )
            .clickable { expandClicked() },
    ) {
        SyncCardHeader(
            folderPairName,
            status,
            hasStalledIssues = hasStalledIssues,
            expanded = expanded,
            expandClicked,
        )

        if (errorRes != null) {
            WarningBanner(
                textString = stringResource(errorRes),
                onCloseClick = null,
                Modifier.padding(top = 16.dp)
            )
            Divider(Modifier.padding(start = 16.dp, end = 16.dp))
        } else {
            Divider(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp))
        }

        AnimatedVisibility(visible = expanded) {
            SyncCardDetailedInfo(deviceStoragePath, megaStoragePath, method)
        }

        SyncCardFooter(
            isSyncRunning = status in arrayOf(
                SyncStatus.SYNCING,
                SyncStatus.SYNCED
            ),
            hasStalledIssues = hasStalledIssues,
            pauseRunClicked,
            removeFolderClicked,
            issuesInfoClicked,
            isLowBatteryLevel = isLowBatteryLevel,
            isError = errorRes != null
        )
    }
}

@Composable
private fun SyncCardHeader(
    folderPairName: String,
    status: SyncStatus,
    hasStalledIssues: Boolean,
    expanded: Boolean,
    expandClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
            .fillMaxWidth()
    ) {
        Row(
            Modifier
                .weight(1f)
                .padding(top = 16.dp)
        ) {
            Image(
                painter = painterResource(IconPackR.drawable.ic_folder_sync_medium_solid),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Column {
                Text(
                    text = folderPairName,
                    style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorPrimary),
                    fontWeight = FontWeight.Medium
                )
                Row(
                    Modifier
                        .padding(top = 8.dp)
                        .height(20.dp)
                ) {
                    Image(
                        painter = when {
                            hasStalledIssues -> painterResource(iconPackR.drawable.ic_alert_circle_regular_medium_outline)
                            status == SyncStatus.SYNCING -> painterResource(coreR.drawable.ic_sync_02)
                            status == SyncStatus.PAUSED -> painterResource(coreR.drawable.ic_pause)
                            else -> painterResource(coreR.drawable.ic_check_circle)
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .align(Alignment.CenterVertically)
                            .size(16.dp),
                        colorFilter = if (hasStalledIssues) {
                            ColorFilter.tint(MaterialTheme.colors.error)
                        } else {
                            ColorFilter.tint(MaterialTheme.colors.textColorPrimary)
                        },
                    )
                    Text(
                        text = when {
                            hasStalledIssues -> stringResource(id = R.string.sync_folders_sync_state_failed)
                            status == SyncStatus.SYNCING -> stringResource(id = R.string.sync_list_sync_state_syncing)
                            status == SyncStatus.PAUSED -> stringResource(id = R.string.sync_list_sync_state_paused)
                            else -> stringResource(id = R.string.sync_list_sync_state_synced)
                        },
                        Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.body2.copy(
                            color = if (hasStalledIssues) {
                                MaterialTheme.colors.error
                            } else {
                                MaterialTheme.colors.textColorPrimary
                            },
                        )
                    )
                }
            }
        }

        Image(
            painter = if (expanded) {
                painterResource(coreR.drawable.ic_chevron_up)
            } else {
                painterResource(coreR.drawable.ic_chevron_down)
            },
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .height(16.dp)
                .width(16.dp)
                .align(Alignment.CenterVertically)
                .clickable { expandClicked() },
            colorFilter = ColorFilter.tint(MaterialTheme.colors.textColorPrimary)
        )
    }
}

@Composable
private fun SyncCardDetailedInfo(
    deviceStoragePath: String,
    megaStoragePath: String,
    method: String,
) {
    Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
        Column {
            Text(
                text = stringResource(id = R.string.sync_folders_device_storage),
                style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.textColorPrimary),
            )
            Text(
                text = deviceStoragePath,
                style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorSecondary),
            )
        }

        Divider(Modifier.padding(top = 16.dp, bottom = 16.dp))

        Column {
            Text(
                text = stringResource(id = R.string.sync_folders_mega_storage),
                style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.textColorPrimary),
            )
            Text(
                text = megaStoragePath,
                style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorSecondary),
            )
        }

        Divider(Modifier.padding(top = 16.dp, bottom = 16.dp))

        Column {
            Text(
                text = stringResource(id = R.string.sync_folders_method),
                style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.textColorPrimary),
            )
            Text(
                text = method,
                style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorSecondary),
            )
        }
        Divider(Modifier.padding(top = 16.dp, end = 16.dp))
    }
}

@Composable
private fun SyncCardFooter(
    isSyncRunning: Boolean,
    hasStalledIssues: Boolean,
    pauseRunClicked: () -> Unit,
    removeFolderClicked: () -> Unit,
    issuesInfoClicked: () -> Unit,
    isLowBatteryLevel: Boolean,
    isError: Boolean,
) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            if (hasStalledIssues) {
                MegaButtonWithIconAndText(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = issuesInfoClicked,
                    icon = coreR.drawable.ic_info,
                    iconColor = MaterialTheme.colors.error,
                    textColor = MaterialTheme.colors.error,
                    text = stringResource(id = R.string.sync_card_sync_issues_info)
                )
            }
            MegaButtonWithIconAndText(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 36.dp),
                onClick = pauseRunClicked,
                icon = if (isSyncRunning) {
                    coreR.drawable.ic_pause
                } else {
                    coreR.drawable.ic_play_circle
                },
                text = if (isSyncRunning) {
                    stringResource(id = R.string.sync_card_pause_sync)
                } else {
                    stringResource(id = R.string.sync_card_run_sync)
                },
                enabled = !isLowBatteryLevel && !isError
            )
            MegaButtonWithIconAndText(
                onClick = removeFolderClicked,
                icon = coreR.drawable.ic_minus_circle,
                text = stringResource(id = R.string.sync_card_remove_sync)
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardExpandedPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SyncCard(
            folderPairName = "Competitors documentation",
            status = SyncStatus.SYNCING,
            hasStalledIssues = false,
            deviceStoragePath = "/storage/emulated/0/Download",
            megaStoragePath = "/Root/Competitors documentation",
            method = "Two-way",
            pauseRunClicked = {},
            expanded = true,
            expandClicked = {},
            removeFolderClicked = {},
            issuesInfoClicked = {},
            isLowBatteryLevel = false,
            errorRes = null,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardExpandedWithBannerPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SyncCard(
            folderPairName = "Competitors documentation",
            status = SyncStatus.SYNCING,
            hasStalledIssues = false,
            deviceStoragePath = "/storage/emulated/0/Download",
            megaStoragePath = "/Root/Competitors documentation",
            method = "Two-way",
            pauseRunClicked = {},
            expanded = true,
            expandClicked = {},
            removeFolderClicked = {},
            issuesInfoClicked = {},
            isLowBatteryLevel = false,
            errorRes = sharedR.string.general_sync_active_sync_below_path,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardCollapsedPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SyncCard(
            folderPairName = "Competitors documentation",
            status = SyncStatus.SYNCING,
            hasStalledIssues = false,
            deviceStoragePath = "/storage/emulated/0/Download",
            megaStoragePath = "/Root/Competitors documentation",
            method = "Two-way",
            pauseRunClicked = {},
            expanded = false,
            expandClicked = {},
            removeFolderClicked = {},
            issuesInfoClicked = {},
            isLowBatteryLevel = false,
            errorRes = null
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardCollapsedWithBannerPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SyncCard(
            folderPairName = "Competitors documentation",
            status = SyncStatus.SYNCING,
            hasStalledIssues = false,
            deviceStoragePath = "/storage/emulated/0/Download",
            megaStoragePath = "/Root/Competitors documentation",
            method = "Two-way",
            pauseRunClicked = {},
            expanded = false,
            expandClicked = {},
            removeFolderClicked = {},
            issuesInfoClicked = {},
            isLowBatteryLevel = false,
            errorRes = sharedR.string.general_sync_active_sync_below_path,
        )
    }
}
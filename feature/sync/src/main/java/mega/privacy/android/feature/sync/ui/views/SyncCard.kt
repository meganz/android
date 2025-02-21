package mega.privacy.android.feature.sync.ui.views

import mega.privacy.android.core.R as coreR
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.shared.original.core.ui.controls.banners.WarningBanner
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaButtonWithIconAndText
import mega.privacy.android.shared.original.core.ui.controls.cards.MegaCard
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.status.MegaStatusIndicator
import mega.privacy.android.shared.original.core.ui.controls.status.StatusColor
import mega.privacy.android.shared.original.core.ui.controls.status.getStatusIconColor
import mega.privacy.android.shared.original.core.ui.controls.status.getStatusTextColor
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.mobile.analytics.event.SyncCardIssuesInfoButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncCardOpenDeviceFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncCardOpenMegaFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncCardPauseRunButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncCardStopButtonPressedEvent

@Composable
internal fun SyncCard(
    sync: SyncUiItem,
    expandClicked: () -> Unit,
    pauseRunClicked: () -> Unit,
    removeFolderClicked: () -> Unit,
    issuesInfoClicked: () -> Unit,
    onOpenDeviceFolderClicked: (String) -> Unit,
    onOpenMegaFolderClicked: () -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    isLowBatteryLevel: Boolean,
    @StringRes errorRes: Int?,
    deviceName: String,
    modifier: Modifier = Modifier,
) {
    MegaCard(
        content = {
            SyncCardHeader(
                syncType = sync.syncType,
                folderPairName = sync.folderPairName,
                status = sync.status,
                hasStalledIssues = sync.hasStalledIssues,
                method = stringResource(id = sync.method)
            )

            if (errorRes != null && errorRes != sharedR.string.general_sync_storage_overquota) {
                WarningBanner(
                    textString = stringResource(errorRes),
                    onCloseClick = null,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            AnimatedVisibility(visible = sync.expanded) {
                SyncCardDetailedInfo(
                    syncType = sync.syncType,
                    deviceStoragePath = sync.deviceStoragePath,
                    megaStoragePath = sync.megaStoragePath,
                    numberOfFiles = sync.numberOfFiles,
                    numberOfFolders = sync.numberOfFolders,
                    totalSizeInBytes = sync.totalSizeInBytes,
                    creationTime = sync.creationTime,
                    deviceName = deviceName,
                    onOpenDeviceFolderClicked = onOpenDeviceFolderClicked,
                )
            }

            SyncCardFooter(
                syncType = sync.syncType,
                isSyncRunning = sync.status in arrayOf(
                    SyncStatus.SYNCING, SyncStatus.SYNCED
                ),
                hasStalledIssues = sync.hasStalledIssues,
                pauseRunClicked = pauseRunClicked,
                removeFolderClicked = removeFolderClicked,
                issuesInfoClicked = issuesInfoClicked,
                onOpenMegaFolderClicked = onOpenMegaFolderClicked,
                onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
                isLowBatteryLevel = isLowBatteryLevel,
                isError = errorRes != null,
                expanded = sync.expanded,
            )
        },
        onClicked = expandClicked,
        modifier = modifier,
    )
}

@Composable
private fun SyncCardHeader(
    syncType: SyncType,
    folderPairName: String,
    status: SyncStatus,
    hasStalledIssues: Boolean,
    method: String,
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
                painter = when (syncType) {
                    SyncType.TYPE_BACKUP -> painterResource(id = IconPackR.drawable.ic_folder_backup_medium_solid)
                    SyncType.TYPE_CAMERA_UPLOADS -> painterResource(id = IconPackR.drawable.ic_folder_camera_uploads_medium_solid)
                    SyncType.TYPE_MEDIA_UPLOADS -> painterResource(id = IconPackR.drawable.ic_folder_camera_uploads_medium_solid)
                    else -> painterResource(IconPackR.drawable.ic_folder_sync_medium_solid)
                },
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Column {
                MegaText(
                    text = folderPairName,
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.subtitle1medium
                )
                MegaStatusIndicator(
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                    statusText = when {
                        hasStalledIssues -> stringResource(id = R.string.sync_folders_sync_state_failed)
                        status == SyncStatus.SYNCING -> when (syncType) {
                            SyncType.TYPE_BACKUP -> stringResource(id = sharedR.string.sync_list_sync_state_updating)
                            else -> stringResource(id = R.string.sync_list_sync_state_syncing)
                        }

                        status == SyncStatus.PAUSED -> stringResource(id = R.string.sync_list_sync_state_paused)
                        status == SyncStatus.DISABLED -> stringResource(id = sharedR.string.sync_list_sync_state_disabled)
                        else -> stringResource(id = sharedR.string.sync_list_sync_state_up_to_date)
                    },
                    statusIcon = when {
                        hasStalledIssues -> iconPackR.drawable.ic_alert_circle_regular_medium_outline
                        status == SyncStatus.SYNCING -> coreR.drawable.ic_sync_02
                        status == SyncStatus.PAUSED -> coreR.drawable.ic_pause
                        status == SyncStatus.DISABLED -> coreR.drawable.ic_alert_triangle
                        else -> coreR.drawable.ic_check_circle
                    },
                    statusColor = when {
                        hasStalledIssues -> StatusColor.Error
                        status == SyncStatus.SYNCING -> StatusColor.Info
                        status == SyncStatus.PAUSED -> null
                        status == SyncStatus.DISABLED -> StatusColor.Warning
                        else -> StatusColor.Success
                    },
                )
                MegaText(
                    text = method,
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.caption,
                )
            }
        }
    }
}

@Composable
private fun SyncCardDetailedInfo(
    syncType: SyncType,
    deviceStoragePath: String,
    megaStoragePath: String,
    numberOfFiles: Int,
    numberOfFolders: Int,
    totalSizeInBytes: Long,
    creationTime: Long,
    deviceName: String,
    onOpenDeviceFolderClicked: (String) -> Unit,
) {
    Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
        InfoRow(
            title = stringResource(id = R.string.sync_folders_device_storage), info = {
                MegaText(
                    text = deviceStoragePath,
                    textColor = TextColor.Accent,
                    modifier = Modifier.clickable {
                        Analytics.tracker.trackEvent(SyncCardOpenDeviceFolderButtonPressedEvent)
                        onOpenDeviceFolderClicked(deviceStoragePath)
                    },
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium)
                )
            }, modifier = Modifier.padding(bottom = 8.dp)
        )

        InfoRow(
            title = stringResource(id = R.string.sync_folders_mega_storage),
            info = when (syncType) {
                SyncType.TYPE_BACKUP -> "Backups/$deviceName/$megaStoragePath"
                else -> megaStoragePath
            },
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
        )

        InfoRow(
            title = stringResource(id = sharedR.string.info_added),
            info = formatModifiedDate(
                locale = java.util.Locale(
                    Locale.current.language, Locale.current.region
                ),
                modificationTime = creationTime,
            ),
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )

        val content = when {
            numberOfFolders != 0 && numberOfFiles != 0 -> pluralStringResource(
                id = sharedR.plurals.info_num_folders_and_files,
                count = numberOfFolders, numberOfFolders,
            ) + pluralStringResource(
                id = sharedR.plurals.info_num_files,
                count = numberOfFiles, numberOfFiles,
            )

            numberOfFiles == 0 -> pluralStringResource(
                id = sharedR.plurals.info_num_folders,
                count = numberOfFolders, numberOfFolders,
            )

            else -> pluralStringResource(
                id = sharedR.plurals.info_num_files,
                count = numberOfFiles, numberOfFiles,
            )
        }
        InfoRow(
            title = stringResource(id = sharedR.string.info_content),
            info = content,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )

        InfoRow(
            title = stringResource(id = sharedR.string.info_total_size),
            info = formatFileSize(
                size = totalSizeInBytes,
                context = LocalContext.current,
            ),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun InfoRow(
    title: String,
    info: String,
    modifier: Modifier = Modifier,
) {
    InfoRow(
        title = title,
        info = {
            MegaText(
                text = info,
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.caption,
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun InfoRow(
    title: String,
    info: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MegaText(
            text = title,
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle2,
        )
        info()
    }
}

@Composable
private fun SyncCardFooter(
    syncType: SyncType,
    isSyncRunning: Boolean,
    hasStalledIssues: Boolean,
    pauseRunClicked: () -> Unit,
    removeFolderClicked: () -> Unit,
    issuesInfoClicked: () -> Unit,
    onOpenMegaFolderClicked: () -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    isLowBatteryLevel: Boolean,
    isError: Boolean,
    expanded: Boolean,
) {
    when {
        isError && !expanded -> MegaDivider(dividerType = DividerType.Centered)
        else -> MegaDivider(
            dividerType = DividerType.Centered,
            modifier = Modifier.padding(top = 16.dp)
        )
    }

    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            if (hasStalledIssues) {
                MegaButtonWithIconAndText(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .defaultMinSize(minWidth = 56.dp, minHeight = 32.dp),
                    onClick = {
                        Analytics.tracker.trackEvent(SyncCardIssuesInfoButtonPressedEvent)
                        issuesInfoClicked()
                    },
                    icon = coreR.drawable.ic_info,
                    iconColor = StatusColor.Error.getStatusIconColor(),
                    textColor = StatusColor.Error.getStatusTextColor(),
                    text = stringResource(id = R.string.sync_card_sync_issues_info)
                )
            }
            MegaButtonWithIconAndText(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .defaultMinSize(minWidth = 56.dp, minHeight = 32.dp),
                onClick = {
                    Analytics.tracker.trackEvent(SyncCardOpenMegaFolderButtonPressedEvent)
                    onOpenMegaFolderClicked()
                },
                icon = iconPackR.drawable.ic_folder_open_medium_regular_outline,
                text = stringResource(id = sharedR.string.general_open_button),
            )
            if (syncType == SyncType.TYPE_CAMERA_UPLOADS || syncType == SyncType.TYPE_MEDIA_UPLOADS) {
                MegaButtonWithIconAndText(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .defaultMinSize(minWidth = 56.dp, minHeight = 32.dp),
                    onClick = onCameraUploadsSettingsClicked,
                    icon = coreR.drawable.ic_gear_six_regular_outline,
                    text = stringResource(id = sharedR.string.general_settings),
                )
            } else {
                MegaButtonWithIconAndText(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .defaultMinSize(minWidth = 56.dp, minHeight = 32.dp),
                    onClick = {
                        Analytics.tracker.trackEvent(SyncCardPauseRunButtonPressedEvent)
                        pauseRunClicked()
                    },
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
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .defaultMinSize(minWidth = 56.dp, minHeight = 32.dp),
                    onClick = {
                        Analytics.tracker.trackEvent(SyncCardStopButtonPressedEvent)
                        removeFolderClicked()
                    },
                    icon = coreR.drawable.ic_minus_circle,
                    text = when (syncType) {
                        SyncType.TYPE_BACKUP -> stringResource(sharedR.string.sync_stop_backup_button)
                        else -> stringResource(sharedR.string.sync_stop_sync_button)
                    },
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardExpandedPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncCard(
            SyncUiItem(
                id = 1234L,
                syncType = syncType,
                folderPairName = "Competitors documentation",
                status = SyncStatus.SYNCING,
                hasStalledIssues = false,
                deviceStoragePath = "/storage/emulated/0/Download",
                megaStoragePath = "Competitors documentation",
                megaStorageNodeId = NodeId(1234L),
                expanded = true,
                numberOfFolders = 5,
                totalSizeInBytes = 23552L,
                creationTime = 1699454365L,
            ),
            pauseRunClicked = {},
            expandClicked = {},
            removeFolderClicked = {},
            issuesInfoClicked = {},
            onOpenDeviceFolderClicked = {},
            onOpenMegaFolderClicked = {},
            onCameraUploadsSettingsClicked = {},
            isLowBatteryLevel = false,
            errorRes = null,
            deviceName = "Device Name",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardExpandedWithBannerPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncCard(
            SyncUiItem(
                id = 1234L,
                syncType = syncType,
                folderPairName = "Competitors documentation",
                status = SyncStatus.SYNCING,
                hasStalledIssues = false,
                deviceStoragePath = "/storage/emulated/0/Download",
                megaStoragePath = "Competitors documentation",
                megaStorageNodeId = NodeId(1234L),
                expanded = true,
                numberOfFolders = 5,
                totalSizeInBytes = 23552L,
                creationTime = 1699454365L,
            ),
            pauseRunClicked = {},
            expandClicked = {},
            removeFolderClicked = {},
            issuesInfoClicked = {},
            onOpenDeviceFolderClicked = {},
            onOpenMegaFolderClicked = {},
            onCameraUploadsSettingsClicked = {},
            isLowBatteryLevel = false,
            errorRes = sharedR.string.general_sync_active_sync_below_path,
            deviceName = "Device Name",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardCollapsedPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncCard(
            SyncUiItem(
                id = 1234L,
                syncType = syncType,
                folderPairName = "Competitors documentation",
                status = SyncStatus.SYNCING,
                hasStalledIssues = false,
                deviceStoragePath = "/storage/emulated/0/Download",
                megaStoragePath = "Competitors documentation",
                megaStorageNodeId = NodeId(1234L),
                expanded = false,
                numberOfFolders = 5,
                totalSizeInBytes = 23552L,
                creationTime = 1699454365L,
            ),
            pauseRunClicked = {},
            expandClicked = {},
            removeFolderClicked = {},
            issuesInfoClicked = {},
            onOpenDeviceFolderClicked = {},
            onOpenMegaFolderClicked = {},
            onCameraUploadsSettingsClicked = {},
            isLowBatteryLevel = false,
            errorRes = null,
            deviceName = "Device Name",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardCollapsedWithBannerPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncCard(
            SyncUiItem(
                id = 1234L,
                syncType = syncType,
                folderPairName = "Competitors documentation",
                status = SyncStatus.SYNCING,
                hasStalledIssues = false,
                deviceStoragePath = "/storage/emulated/0/Download",
                megaStoragePath = "Competitors documentation",
                megaStorageNodeId = NodeId(1234L),
                expanded = false,
                numberOfFolders = 5,
                totalSizeInBytes = 23552L,
                creationTime = 1699454365L,
            ),
            pauseRunClicked = {},
            expandClicked = {},
            removeFolderClicked = {},
            issuesInfoClicked = {},
            onOpenDeviceFolderClicked = {},
            onOpenMegaFolderClicked = {},
            onCameraUploadsSettingsClicked = {},
            isLowBatteryLevel = false,
            errorRes = sharedR.string.general_sync_active_sync_below_path,
            deviceName = "Device Name",
        )
    }
}
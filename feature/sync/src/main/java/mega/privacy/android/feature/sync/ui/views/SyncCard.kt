package mega.privacy.android.feature.sync.ui.views

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.sync.domain.entity.SyncStatus

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
            expandClicked
        )

        Divider(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp))

        AnimatedVisibility(visible = expanded) {
            SyncCardDetailedInfo(deviceStoragePath, megaStoragePath, method)
        }

        SyncCardFooter(
            isSyncRunning = status in arrayOf(
                SyncStatus.SYNCING,
                SyncStatus.SYNCED
            ),
            isError = hasStalledIssues,
            pauseRunClicked,
            removeFolderClicked
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
                painter = painterResource(R.drawable.ic_folder_sync),
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
                            hasStalledIssues -> painterResource(R.drawable.ic_alert_circle)
                            status == SyncStatus.SYNCING -> painterResource(R.drawable.ic_sync_02)
                            status == SyncStatus.PAUSED -> painterResource(R.drawable.ic_pause)
                            else -> painterResource(R.drawable.ic_check_circle)
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
                            hasStalledIssues -> "Failed"
                            status == SyncStatus.SYNCING -> "Syncing..."
                            status == SyncStatus.PAUSED -> "Paused"
                            else -> "Synced"
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
                painterResource(R.drawable.ic_chevron_up)
            } else {
                painterResource(R.drawable.ic_chevron_down)
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
                text = "Device storage",
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
                text = "MEGA storage",
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
                text = "Method",
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
    isError: Boolean,
    pauseRunClicked: () -> Unit,
    removeFolderClicked: () -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            if (isError) {
                IconButtonWithText(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = removeFolderClicked,
                    icon = R.drawable.ic_info,
                    color = MaterialTheme.colors.error,
                    text = "Issues info"
                )
            }
            IconButtonWithText(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 36.dp),
                onClick = pauseRunClicked,
                icon = if (isSyncRunning) {
                    R.drawable.ic_pause
                } else {
                    R.drawable.ic_play_circle
                },
                text = if (isSyncRunning) {
                    "Pause"
                } else {
                    "Run"
                }
            )
            IconButtonWithText(
                onClick = removeFolderClicked,
                icon = R.drawable.ic_minus_circle,
                text = "Remove"
            )
        }
    }
}

@Composable
private fun IconButtonWithText(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    text: String,
    color: Color = MaterialTheme.colors.textColorPrimary,
    onClick: () -> Unit,
) {
    Column(modifier.clickable { onClick() }) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .height(16.dp)
                .width(16.dp)
                .align(Alignment.CenterHorizontally),
            colorFilter = ColorFilter.tint(color)
        )
        Text(
            text = text,
            Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.caption.copy(color = color),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardExpandedPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
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
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncCardCollapsedPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
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
        )
    }
}

@CombinedThemePreviews
@Composable
private fun IconButtonPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        IconButtonWithText(
            onClick = {}, icon = R.drawable.ic_info, text = "Info"
        )
    }
}
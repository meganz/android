package mega.privacy.android.feature.sync.ui.views

import android.content.res.Configuration
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.feature.sync.ui.model.SyncStatus

@Composable
internal fun SyncCard(
    modifier: Modifier = Modifier,
    folderPairName: String,
    status: SyncStatus,
    deviceStoragePath: String,
    megaStoragePath: String,
    method: String,
    expanded: Boolean,
    expandClicked: () -> Unit,
    infoClicked: () -> Unit,
    removeFolderClicked: () -> Unit,
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
            ),
    ) {
        SyncCardHeader(
            folderPairName, status, expanded, expandClicked
        )

        Divider(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp))

        AnimatedVisibility(visible = expanded) {
            SyncCardDetailedInfo(deviceStoragePath, megaStoragePath, method)
        }

        SyncCardFooter(infoClicked, removeFolderClicked)
    }
}

@Composable
private fun SyncCardHeader(
    folderPairName: String,
    status: SyncStatus,
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
                        painter = if (status == SyncStatus.SYNCING) {
                            painterResource(R.drawable.ic_sync_02)
                        } else {
                            painterResource(R.drawable.ic_check_circle)
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .align(Alignment.CenterVertically),
                        colorFilter = ColorFilter.tint(MaterialTheme.colors.textColorPrimary)
                    )
                    Text(
                        text = if (status == SyncStatus.SYNCING) {
                            "Syncing..."
                        } else {
                            "Synced"
                        },
                        Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorSecondary),
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
private fun SyncCardFooter(infoClicked: () -> Unit, removeFolderClicked: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, top = 16.dp)
        ) {
            IconButtonWithText(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 36.dp),
                onClick = infoClicked,
                icon = R.drawable.ic_info,
                text = "Info"
            )
            IconButtonWithText(
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                onClick = removeFolderClicked,
                icon = R.drawable.ic_minus_circle,
                text = "Remove synced folder"
            )
        }
    }
}

@Composable
private fun IconButtonWithText(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    text: String,
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
            colorFilter = ColorFilter.tint(MaterialTheme.colors.textColorPrimary)
        )
        Text(
            text = text,
            Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.textColorPrimary),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SyncCardExpandedPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SyncCard(
            folderPairName = "Competitors documentation",
            status = SyncStatus.SYNCING,
            deviceStoragePath = "/storage/emulated/0/Download",
            megaStoragePath = "/Root/Competitors documentation",
            method = "Two-way",
            infoClicked = {},
            expanded = true,
            expandClicked = {},
            removeFolderClicked = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SyncCardCollapsedPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SyncCard(
            folderPairName = "Competitors documentation",
            status = SyncStatus.SYNCING,
            deviceStoragePath = "/storage/emulated/0/Download",
            megaStoragePath = "/Root/Competitors documentation",
            method = "Two-way",
            infoClicked = {},
            expanded = false,
            expandClicked = {},
            removeFolderClicked = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IconButtonPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        IconButtonWithText(
            onClick = {}, icon = R.drawable.ic_info, text = "Info"
        )
    }
}
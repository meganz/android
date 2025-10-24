package mega.privacy.android.feature.devicecenter.ui.view

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.ThemedSurface
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterInfoUiState
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Test tag for the Device Center Info View icon in M3
 */
internal const val DEVICE_CENTER_INFO_VIEW_ICON_TAG_M3 = "device_center_info_view_m3:icon_tag"

/**
 * Material 3 version of the Device Center Info Screen
 *
 * This version does NOT include its own AppBar - it relies on the parent's DeviceCenterAppBarM3
 * to display the correct title.
 *
 * @param uiState The UI State
 * @param onBackPressHandled Lambda that performs a specific action when the Composable handles the Back Press
 * @param paddingValues Padding values from the parent scaffold
 */
@Composable
internal fun DeviceCenterInfoScreenM3(
    uiState: DeviceCenterInfoUiState,
    onBackPressHandled: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
) {
    // Handle hardware/gesture back press - this takes precedence over parent BackHandlers
    BackHandler(enabled = true) {
        onBackPressHandled()
    }

    ThemedSurface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        DeviceCenterInfoScreenContentM3(
            uiState = uiState,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun DeviceCenterInfoScreenContentM3(
    uiState: DeviceCenterInfoUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        IconAndTitleRowM3(
            icon = uiState.icon,
            applySecondaryColorIconTint = uiState.applySecondaryColorIconTint,
            name = uiState.name
        )
        if (uiState.creationTime > 0) {
            InfoRowM3(
                title = stringResource(id = sharedR.string.info_added),
                info = formatModifiedDate(
                    locale = java.util.Locale(
                        Locale.current.language, Locale.current.region
                    ),
                    modificationTime = uiState.creationTime,
                )
            )
        }
        if (uiState.numberOfFolders > 0 || uiState.numberOfFiles > 0) {
            val content = when {
                uiState.numberOfFolders != 0 && uiState.numberOfFiles != 0 -> pluralStringResource(
                    id = sharedR.plurals.info_num_folders_and_files,
                    count = uiState.numberOfFolders, uiState.numberOfFolders,
                ) + pluralStringResource(
                    id = sharedR.plurals.info_num_files,
                    count = uiState.numberOfFiles, uiState.numberOfFiles,
                )

                uiState.numberOfFiles == 0 -> pluralStringResource(
                    id = sharedR.plurals.info_num_folders,
                    count = uiState.numberOfFolders, uiState.numberOfFolders,
                )

                else -> pluralStringResource(
                    id = sharedR.plurals.info_num_files,
                    count = uiState.numberOfFiles, uiState.numberOfFiles,
                )
            }
            InfoRowM3(
                title = stringResource(id = sharedR.string.info_content),
                info = content,
            )
        }
        if (uiState.totalSizeInBytes > 0) {
            InfoRowM3(
                title = stringResource(id = sharedR.string.info_total_size),
                info = formatFileSize(
                    size = uiState.totalSizeInBytes,
                    context = LocalContext.current
                )
            )
        }
    }
}

@Composable
private fun IconAndTitleRowM3(
    @DrawableRes icon: Int,
    applySecondaryColorIconTint: Boolean,
    name: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MegaIcon(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .testTag(DEVICE_CENTER_INFO_VIEW_ICON_TAG_M3),
            painter = painterResource(id = icon),
            contentDescription = "Item icon",
            tint = if (applySecondaryColorIconTint) IconColor.Secondary else IconColor.Primary,
        )

        MegaText(
            text = name,
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun InfoRowM3(
    title: String,
    info: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
    ) {
        MegaText(
            text = title,
            textColor = TextColor.Primary,
            modifier = Modifier.padding(bottom = 2.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        MegaText(
            text = info,
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DeviceCenterInfoScreenM3DevicePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterInfoScreenM3(
            uiState = DeviceCenterInfoUiState(
                icon = IconPackR.drawable.ic_pc_medium_solid,
                applySecondaryColorIconTint = true,
                name = "Device name",
                numberOfFiles = 6,
                numberOfFolders = 5,
                totalSizeInBytes = 23552L,
            ),
            onBackPressHandled = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DeviceCenterInfoScreenM3FolderPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterInfoScreenM3(
            uiState = DeviceCenterInfoUiState(
                icon = IconPackR.drawable.ic_folder_medium_solid,
                name = "Folder name",
                numberOfFiles = 6,
                numberOfFolders = 5,
                totalSizeInBytes = 23552L,
                creationTime = 1699454365L,
            ),
            onBackPressHandled = {},
        )
    }
}

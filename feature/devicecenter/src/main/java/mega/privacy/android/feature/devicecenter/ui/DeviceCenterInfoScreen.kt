package mega.privacy.android.feature.devicecenter.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.feature.devicecenter.ui.model.DeviceCenterInfoUiState
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Test tags for the Device Center Info View
 */
internal const val DEVICE_CENTER_INFO_VIEW_TOOLBAR = "device_center_info_view:mega_app_bar"
internal const val DEVICE_CENTER_INFO_VIEW_ICON_TAG = "device_center_info_view:icon_tag"

/**
 * A [Composable] that serves as the Info Screen for items (Devices or Folders) in the Device Center
 *
 * @param uiState The UI State
 * @param onBackPressHandled Lambda that performs a specific action when the Composable handles the Back Press
 */
@Composable
internal fun DeviceCenterInfoScreen(
    uiState: DeviceCenterInfoUiState,
    onBackPressHandled: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(DEVICE_CENTER_INFO_VIEW_TOOLBAR),
                appBarType = AppBarType.BACK_NAVIGATION,
                title = String(),
                elevation = 0.dp,
                onNavigationPressed = { onBackPressHandled() },
            )
        },
        content = { paddingValues ->
            DeviceCenterInfoScreenContent(
                uiState = uiState,
                modifier = Modifier.padding(paddingValues),
            )
        },
    )
}

@Composable
private fun DeviceCenterInfoScreenContent(
    uiState: DeviceCenterInfoUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        IconAndTitleRow(
            icon = uiState.icon,
            applySecondaryColorIconTint = uiState.applySecondaryColorIconTint,
            name = uiState.name
        )
        if (uiState.creationTime > 0) {
            InfoRow(
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
            InfoRow(
                title = stringResource(id = sharedR.string.info_content),
                info = content,
            )
        }
        if (uiState.totalSizeInBytes > 0) {
            InfoRow(
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
private fun IconAndTitleRow(
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
        Image(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .testTag(DEVICE_CENTER_INFO_VIEW_ICON_TAG),
            painter = painterResource(id = icon),
            contentDescription = "Item icon",
            colorFilter = if (applySecondaryColorIconTint) {
                // Temporary fix in order to fix icon color until we change to the new icon set.
                // Will be removed soon
                ColorFilter.tint(MaterialTheme.colors.textColorSecondary)
            } else {
                null
            }
        )

        MegaText(
            text = name,
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@Composable
private fun InfoRow(
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
            style = MaterialTheme.typography.subtitle1
        )
        MegaText(
            text = info,
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.subtitle2
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DeviceCenterInfoScreenDevicePreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterInfoScreen(
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
private fun DeviceCenterInfoScreenFolderPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterInfoScreen(
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
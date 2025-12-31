package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun CloudDriveEmptyView(
    onAddItemsClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isRootCloudDrive: Boolean = false,
    showAddItems: Boolean = true,
) {
    val imageDrawable = if (isRootCloudDrive) {
        iconPackR.drawable.ic_usp_2
    } else {
        iconPackR.drawable.ic_empty_folder
    }

    val titleId = if (isRootCloudDrive) {
        sharedR.string.context_empty_cloud_drive_title
    } else {
        sharedR.string.context_empty_folder_title
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = modifier.padding(LocalSpacing.current.x16),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmptyImage(imageDrawable = imageDrawable, modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.width(24.dp))

            EmptyContent(
                modifier = Modifier.weight(1f),
                titleId = titleId,
                onAddItemsClicked = onAddItemsClicked,
                showAddItems = showAddItems
            )
        }
    } else {
        Column(
            modifier = modifier.padding(LocalSpacing.current.x16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EmptyImage(imageDrawable = imageDrawable)

            Spacer(modifier = Modifier.height(24.dp))

            EmptyContent(
                titleId = titleId,
                onAddItemsClicked = onAddItemsClicked,
                showAddItems = showAddItems
            )
        }
    }
}

@Composable
private fun EmptyImage(
    imageDrawable: Int,
    modifier: Modifier = Modifier,
) {
    Image(
        modifier = modifier
            .size(120.dp)
            .testTag(EMPTY_IMAGE_TAG),
        painter = painterResource(id = imageDrawable),
        contentDescription = "Empty icon"
    )
}

@Composable
private fun EmptyContent(
    titleId: Int,
    onAddItemsClicked: () -> Unit,
    modifier: Modifier = Modifier,
    showAddItems: Boolean = true,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MegaText(
            modifier = Modifier.testTag(EMPTY_TITLE_TAG),
            text = stringResource(titleId),
            textColor = TextColor.Primary,
            style = AppTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        if (showAddItems) {
            Spacer(modifier = Modifier.height(16.dp))

            MegaText(
                modifier = Modifier.testTag(EMPTY_DESCRIPTION_TAG),
                text = stringResource(sharedR.string.context_empty_cloud_drive_description),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryFilledButton(
                modifier = Modifier
                    .wrapContentSize()
                    .testTag(ADD_ITEMS_BUTTON_TAG),
                text = stringResource(sharedR.string.album_content_action_add_items),
                leadingIcon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Plus),
                onClick = onAddItemsClicked
            )
        }
    }
}


@CombinedThemePreviews
@Preview(
    name = "Landscape",
    showBackground = true,
    device = "spec:parent=pixel_5,orientation=landscape"
)
@Composable
private fun CloudDriveEmptyViewPreview() {
    AndroidThemeForPreviews {
        CloudDriveEmptyView(
            isRootCloudDrive = true,
            onAddItemsClicked = { }
        )
    }
}

@CombinedThemePreviews
@Preview(
    name = "Landscape",
    showBackground = true,
    device = "spec:parent=pixel_5,orientation=landscape"
)
@Composable
private fun FolderEmptyViewPreview() {
    AndroidThemeForPreviews {
        CloudDriveEmptyView(
            isRootCloudDrive = false,
            onAddItemsClicked = { }
        )
    }
}

@CombinedThemePreviews
@Preview(
    name = "Landscape",
    showBackground = true,
    device = "spec:parent=pixel_5,orientation=landscape"
)
@Composable
private fun FolderEmptyNoWritePermissionViewPreview() {
    AndroidThemeForPreviews {
        CloudDriveEmptyView(
            isRootCloudDrive = false,
            showAddItems = false,
            onAddItemsClicked = { }
        )
    }
}

internal const val EMPTY_IMAGE_TAG = "cloud_drive_empty_view:image"
internal const val EMPTY_TITLE_TAG = "cloud_drive_empty_view:title"
internal const val EMPTY_DESCRIPTION_TAG = "cloud_drive_empty_view:description"
internal const val ADD_ITEMS_BUTTON_TAG = "cloud_drive_empty_view:add_items_button"


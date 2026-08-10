package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
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
    val illustrationId = if (isRootCloudDrive) {
        iconPackR.drawable.ic_usp_2
    } else {
        iconPackR.drawable.ic_empty_folder
    }

    val titleId = if (isRootCloudDrive) {
        sharedR.string.context_empty_cloud_drive_title
    } else {
        sharedR.string.context_empty_folder_title
    }

    EmptyStateView(
        imagePainter = painterResource(id = illustrationId),
        title = stringResource(titleId),
        modifier = modifier.testTag(EMPTY_VIEW_TAG),
        description = if (showAddItems) {
            SpannableText(stringResource(sharedR.string.context_empty_cloud_drive_description))
        } else {
            null
        },
        primaryAction = if (showAddItems) {
            @Composable {
                if (showAddItems) {
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
        } else {
            null
        },
    )
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

internal const val EMPTY_VIEW_TAG = "cloud_drive_empty_view:empty_state"
internal const val ADD_ITEMS_BUTTON_TAG = "cloud_drive_empty_view:add_items_button"

package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
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
    modifier: Modifier = Modifier,
    isRootCloudDrive: Boolean = false,
    onAddItemsClicked: () -> Unit
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

    EmptyStateView(
        modifier = modifier,
        illustration = imageDrawable,
        title = stringResource(titleId),
        description = SpannableText(
            text = stringResource(sharedR.string.context_empty_cloud_drive_description)
        ),
        actions = {
            PrimaryFilledButton(
                modifier = Modifier.wrapContentSize(),
                text = stringResource(sharedR.string.album_content_action_add_items),
                leadingIcon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Plus),
                onClick = onAddItemsClicked
            )
        }
    )
}

@CombinedThemePreviews
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
@Composable
private fun FolderEmptyViewPreview() {
    AndroidThemeForPreviews {
        CloudDriveEmptyView(
            isRootCloudDrive = false,
            onAddItemsClicked = { }
        )
    }
}

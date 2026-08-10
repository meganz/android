package mega.privacy.android.feature.photos.presentation.albums.content

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.presentation.albums.content.model.AlbumContentSelectionAction
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun AlbumContentEmptyLayout(
    modifier: Modifier = Modifier,
    isActionVisible: Boolean = false,
    onAddPhotosClicked: () -> Unit,
) {
    val action = AlbumContentSelectionAction.AddItems

    EmptyStateView(
        modifier = modifier,
        imagePainter = painterResource(id = R.drawable.il_album_image),
        title = stringResource(sharedR.string.album_content_empty_album_title),
        primaryAction = if (isActionVisible) {
            @Composable {
                PrimaryFilledButton(
                    modifier = Modifier.wrapContentSize(),
                    text = action.getDescription(),
                    leadingIcon = action.getIconPainter(),
                    onClick = onAddPhotosClicked
                )
            }
        } else {
            null
        },
    )
}

@CombinedThemePreviews
@Composable
private fun AlbumContentEmptyLayoutPreview() {
    AndroidThemeForPreviews {
        AlbumContentEmptyLayout(
            modifier = Modifier.fillMaxSize(),
            onAddPhotosClicked = {}
        )
    }
}


package mega.privacy.android.feature.photos.presentation.albums.content

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.presentation.albums.content.model.AlbumContentSelectionAction
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun AlbumContentEmptyLayout(
    modifier: Modifier = Modifier,
    onAddPhotosClicked: () -> Unit,
) {
    val action = AlbumContentSelectionAction.AddItems

    EmptyStateView(
        modifier = modifier,
        illustration = R.drawable.il_album_image,
        description = SpannableText(
            text = stringResource(sharedR.string.album_content_empty_album_title)
        ),
        actions = {
            PrimaryFilledButton(
                modifier = Modifier.wrapContentSize(),
                text = action.getDescription(),
                leadingIcon = action.getIconPainter(),
                onClick = onAddPhotosClicked
            )
        }
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


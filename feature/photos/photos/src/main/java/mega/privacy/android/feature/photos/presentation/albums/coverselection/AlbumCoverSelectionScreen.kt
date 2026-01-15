package mega.privacy.android.feature.photos.presentation.albums.coverselection

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.surface.RowSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.component.PhotosNodeGridView
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AlbumCoverSelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumCoverSelectionViewModel =
        hiltViewModel<AlbumCoverSelectionViewModel, AlbumCoverSelectionViewModel.Factory>(
            creationCallback = { it.create(null) }
        ),
    onBackClicked: () -> Unit = {},
    onCompletion: (message: String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lazyGridState = rememberLazyGridState()

    if (state.isInvalidAlbum) onBackClicked()

    HandleAlbumCoverCompletion(
        isSelectionCompleted = state.isSelectionCompleted,
        onCompletion = onCompletion,
    )

    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            MegaTopAppBar(
                navigationType = AppBarNavigationType.Back(onBackClicked),
                title = stringResource(id = sharedR.string.album_content_selection_action_select_album_cover_description),
            )
        },
        content = { padding ->
            PhotosNodeGridView(
                modifier = Modifier.fillMaxSize(),
                items = state.photosNodeContentTypes,
                gridSize = TimelineGridSize.Default,
                onGridSizeChange = {},
                onClick = { viewModel.selectPhoto(it.photo) },
                onLongClick = { viewModel.selectPhoto(it.photo) },
                lazyGridState = lazyGridState,
                contentPadding = padding
            )
        },
        bottomBar = {
            AlbumCoverSelectionFooter(
                hasSelectedPhoto = state.hasSelectedPhoto,
                onBackClicked = onBackClicked,
                onUpdateCover = viewModel::updateCover,
            )
        }
    )
}

@Composable
private fun AlbumCoverSelectionFooter(
    hasSelectedPhoto: Boolean,
    onBackClicked: () -> Unit,
    onUpdateCover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RowSurface(
        modifier = modifier.fillMaxWidth(),
        surfaceColor = SurfaceColor.PageBackground
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            MegaText(
                modifier = Modifier
                    .clickable { onBackClicked() }
                    .align(Alignment.CenterVertically),
                text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                textColor = TextColor.Accent,
                style = AppTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.size(28.dp))

            PrimaryFilledButton(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterVertically),
                enabled = hasSelectedPhoto,
                onClick = onUpdateCover,
                text = stringResource(sharedR.string.general_action_save)
            )
        }
    }
}

@Composable
private fun HandleAlbumCoverCompletion(
    isSelectionCompleted: Boolean,
    onCompletion: (message: String) -> Unit,
) {
    if (isSelectionCompleted) {
        val message = stringResource(sharedR.string.album_cover_selected_success_message)
        onCompletion(message)
    }
}

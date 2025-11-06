package mega.privacy.android.feature.photos.presentation.albums.content

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.albums.view.AlbumDynamicContentGrid

@Composable
fun AlbumContentRoute(
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    viewModel: AlbumContentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    AlbumContentScreen(
        uiState = uiState,
        onBack = onBack,
        togglePhotoSelection = viewModel::togglePhotoSelection
    )
}

// Todo implement Album Content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumContentScreen(
    uiState: AlbumContentUiState,
    onBack: () -> Unit,
    togglePhotoSelection: (PhotoUiState) -> Unit,
) {
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val smallWidth = remember(configuration) {
        (configuration.screenWidthDp.dp - 1.dp) / 3
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaTopAppBar(
                navigationType = AppBarNavigationType.Back(onBack),
                title = uiState.uiAlbum?.title?.getTitleString(context).orEmpty()
            )
        },
    ) { innerPadding ->
        AlbumDynamicContentGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            lazyListState = lazyListState,
            photos = uiState.photos,
            smallWidth = smallWidth,
            onClick = { photo ->
                if (uiState.selectedPhotos.isEmpty()) {
                    //onNavigatePhotoPreview(photo, photos)
                } else {
                    togglePhotoSelection(photo)
                }
            },
            onLongPress = { photo ->
                togglePhotoSelection(photo)
            },
            selectedPhotos = uiState.selectedPhotos,
            shouldApplySensitiveMode = uiState.hiddenNodeEnabled
                    && uiState.accountType?.isPaid == true
                    && !uiState.isBusinessAccountExpired,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AlbumContentScreenPreview() {
    AndroidThemeForPreviews {
        AlbumContentScreen(
            uiState = AlbumContentUiState(),
            onBack = {},
            togglePhotoSelection = {}
        )
    }
}
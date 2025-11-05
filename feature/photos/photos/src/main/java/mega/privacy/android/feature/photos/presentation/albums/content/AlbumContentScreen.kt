package mega.privacy.android.feature.photos.presentation.albums.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

@Composable
fun AlbumContentRoute(
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    viewModel: AlbumContentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    AlbumContentScreen(uiState, onBack)
}

// Todo implement Album Content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumContentScreen(
    uiState: AlbumContentState,
    onBack: () -> Unit,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaTopAppBar(
                navigationType = AppBarNavigationType.Back(onBack),
                title = "Album Content"
            )
        },
    ) { innerPadding ->
        // Todo implement Album Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MegaText("${uiState.photos.size} photos")
        }
    }
}

@CombinedThemePreviews
@Composable
private fun AlbumContentScreenPreview() {
    AndroidThemeForPreviews {
        AlbumContentScreen(AlbumContentState()) { }
    }
}
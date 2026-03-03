package mega.privacy.android.feature.clouddrive.presentation.folderlink

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.CLOUD_DRIVE_MAIN_APP_BAR_TAG
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkUiState
import mega.privacy.android.navigation.destination.TransfersNavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderLinkScreen(
    viewModel: FolderLinkViewModel,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FolderLinkContent(
        uiState = uiState,
        onNavigate = onNavigate,
        onBack = onBack,
        onAction = viewModel::processAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderLinkContent(
    uiState: FolderLinkUiState,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onAction: (FolderLinkAction) -> Unit,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(CLOUD_DRIVE_MAIN_APP_BAR_TAG),
                title = "Folder Link",
                navigationType = AppBarNavigationType.Back(onBack),
                trailingIcons = {
                    TransfersToolbarWidget {
                        onNavigate(TransfersNavKey())
                    }
                },
            )
        },
        bottomBar = {},
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (val contentState = uiState.contentState) {
                FolderLinkContentState.Loading -> CircularProgressIndicator()

                is FolderLinkContentState.DecryptionKeyRequired -> {
                    // TODO: show decryption key dialog in later MR
                    MegaText(text = if (contentState.isKeyIncorrect) "Invalid decryption key" else "Decryption key required")
                }

                FolderLinkContentState.Expired ->
                    MegaText(text = "This link has expired")

                FolderLinkContentState.Unavailable ->
                    MegaText(text = "This link is unavailable")

                is FolderLinkContentState.Loaded ->
                    // TODO: render node list in later MR
                    MegaText(text = "Folder Link")
            }
        }
    }
}

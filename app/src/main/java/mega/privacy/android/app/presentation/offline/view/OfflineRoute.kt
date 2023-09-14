package mega.privacy.android.app.presentation.offline.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.offline.offlinecompose.OfflineComposeViewModel
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineUIState
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

internal object OfflineViewTestTags {
    //To add all the required test tags for the offline screen
}

@Composable
internal fun OfflineRoute(viewModel: OfflineComposeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OfflineScreen(uiState = uiState)
}

@Composable
fun OfflineScreen(
    uiState: OfflineUIState,
    modifier: Modifier = Modifier,
) {
    // To add the needed compose implementation: Ticket: AND-17195
}

@CombinedThemePreviews
@Composable
private fun OfflineViewPreview() {
    // To add the needed compose implementation: Ticket: AND-17195
}
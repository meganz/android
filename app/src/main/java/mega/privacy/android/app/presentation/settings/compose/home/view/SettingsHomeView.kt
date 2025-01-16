package mega.privacy.android.app.presentation.settings.compose.home.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.recentactions.view.RecentLoadingView
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingsUiState

@Composable
internal fun SettingsHomeView(
    state: SettingsUiState,
    initialKey: String?,
    navHostController: NavHostController,
) {
    when (state) {
        is SettingsUiState.Data -> DataView(
            state = state,
            initialKey = initialKey,
            navHostController = navHostController
        )

        SettingsUiState.Loading -> LoadingView()
    }
}

@Composable
private fun DataView(
    state: SettingsUiState.Data,
    initialKey: String?,
    navHostController: NavHostController,
    modifier: Modifier = Modifier,
) {
    SettingsListView(
        data = state,
        modifier = modifier,
        initialKey = initialKey,
        navHostController = navHostController,
    )
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    RecentLoadingView(modifier) // Placeholder for a settings loading view
}

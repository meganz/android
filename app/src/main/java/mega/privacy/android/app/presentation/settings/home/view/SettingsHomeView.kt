package mega.privacy.android.app.presentation.settings.home.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.recentactions.view.RecentLoadingView
import mega.privacy.android.app.presentation.settings.home.model.SettingsUiState

@Composable
internal fun SettingsHomeView(
    onBackPressed: () -> Unit,
    state: SettingsUiState,
    initialKey: String?,
) {
    MegaScaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            MegaTopAppBar(
                navigationType = AppBarNavigationType.Back(onBackPressed),
                title = stringResource(
                    R.string.action_settings
                ),
            )
        },
        snackbarHost = {},
        bottomBar = {},
        content = { padding ->
            when (state) {
                is SettingsUiState.Data -> DataView(
                    state,
                    Modifier.padding(padding),
                    initialKey
                )

                SettingsUiState.Loading -> LoadingView(Modifier.padding(padding))
            }
        }
    )
}

@Composable
private fun DataView(
    state: SettingsUiState.Data,
    modifier: Modifier,
    initialKey: String?,
) {
    SettingsListView(
        data = state,
        modifier = modifier,
        initialKey = initialKey,
    )
}

@Composable
private fun LoadingView(modifier: Modifier) {
    RecentLoadingView(modifier) // Placeholder for a settings loading view
}

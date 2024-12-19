package mega.privacy.android.app.presentation.settings.home.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R

@Composable
internal fun SettingsHomeView(
    onBackPressed: () -> Unit,
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
            ColumnView(padding)
        }
    )
}

@Composable
private fun ColumnView(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MegaText("New Settings Activity", TextColor.Primary)
    }
}

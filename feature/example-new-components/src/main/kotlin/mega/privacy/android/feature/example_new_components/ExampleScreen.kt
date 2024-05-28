package mega.privacy.android.feature.example_new_components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor

@Composable
fun ExampleScreen() {
    val isDark = isSystemInDarkTheme() //this should come from the use-case
    AndroidTheme(isDark) {
        ExampleView()
    }
}

@Composable
fun ExampleView() {
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    MegaScaffold(
        modifier = Modifier,
        snackbarHost = {
            MegaSnackbar(snackbarHostState)
        },
        topBar = {
            MegaTopAppBar(title = "Example screen")
        },
        bottomBar = { MegaText(text = "bottom", textColor = TextColor.Primary) },
        content = {
            Column(
                Modifier
                    .padding(it)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MegaText(text = "This is an example text", textColor = TextColor.Accent)
                MegaOutlinedButton(modifier = Modifier, onClick = { /*TODO*/ }, text = "Button")
                MegaText(text = "This is an example error", textColor = TextColor.Error)
            }
        }
    )
}

@Composable
@CombinedThemePreviews
private fun MainComposeViewPreview() {
    AndroidThemeForPreviews {
        ExampleView()
    }
}
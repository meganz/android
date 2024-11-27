package mega.privacy.android.app.presentation.login.createaccount.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CreateAccountRoute(
    modifier: Modifier = Modifier,
) {
    CreateAccountScreen(
        modifier = modifier.semantics { testTagsAsResourceId = true },
    )
}

@Composable
internal fun CreateAccountScreen(
    modifier: Modifier = Modifier,
) {
    MegaText(
        text = "Create Account Screen",
        textColor = TextColor.Primary,
        modifier = modifier,
    )

}

@CombinedThemePreviews
@Composable
private fun CreateAccountScreenPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CreateAccountScreen(
            modifier = Modifier.fillMaxSize(),
        )
    }
}

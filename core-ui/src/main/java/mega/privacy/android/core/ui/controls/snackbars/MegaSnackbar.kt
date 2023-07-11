package mega.privacy.android.core.ui.controls.snackbars

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_white
import mega.privacy.android.core.ui.theme.extensions.teal_200_teal_300


internal const val SNACKBAR_TEST_TAG = "mega_snackbar:snackbar_body"

/**
 * Snack bar following Mega theme
 */
@Composable
fun MegaSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
) = Snackbar(
    modifier = modifier.testTag(SNACKBAR_TEST_TAG),
    snackbarData = snackbarData,
    actionOnNewLine = actionOnNewLine,
    backgroundColor = MaterialTheme.colors.grey_alpha_087_white,
    actionColor = MaterialTheme.colors.teal_200_teal_300,
)

@CombinedThemePreviews
@Composable
private fun MegaSnackbarPreview(
    @PreviewParameter(MegaSnackbarProvider::class) data: Pair<SnackbarData, Boolean>,
) {
    AndroidTheme(isSystemInDarkTheme()) {
        MegaSnackbar(snackbarData = data.first, actionOnNewLine = data.second)
    }
}

private class MegaSnackbarProvider : PreviewParameterProvider<Pair<SnackbarData, Boolean>> {
    override val values = listOf(
        createSnackbarData("Hello, this is something important") to false,
        createSnackbarData("Hello, this is something very important", "Action") to false,
        createSnackbarData(
            "Hello, this is something very important and very, really and actually long",
            "Action"
        ) to true
    ).asSequence()
}

internal fun createSnackbarData(message: String, action: String? = null): SnackbarData =
    object : SnackbarData {
        override val message: String = message
        override val actionLabel: String? = action
        override val duration = SnackbarDuration.Indefinite
        override fun performAction() {}
        override fun dismiss() {}
    }
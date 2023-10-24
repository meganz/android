package mega.privacy.android.core.ui.controls.snackbars

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.utils.composeLet


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
    modifier = modifier
        .testTag(SNACKBAR_TEST_TAG)
        .padding(12.dp),
    content = { SnackBarText(snackbarData.message) },
    action = snackbarData.actionLabel?.composeLet {
        SnackbarButton(text = it, onClick = { snackbarData.performAction() })
    },
    actionOnNewLine = actionOnNewLine,
    shape = MaterialTheme.shapes.small,
    backgroundColor = MegaTheme.colors.components.toastBackground,
    contentColor = MegaTheme.colors.text.inverse,
    elevation = 8.dp
)

@Composable
private fun SnackBarText(text: String) = Text(
    text = text,
    color = MegaTheme.colors.text.inverse,
    style = MaterialTheme.typography.subtitle2,
)

@Composable
private fun SnackbarButton(
    text: String,
    onClick: () -> Unit,
) = TextButton(
    onClick = onClick,
    colors = MegaTheme.colors.snackBarButtonColors,
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
) {
    Text(
        text = text,
        style = MaterialTheme.typography.button
    )
}

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
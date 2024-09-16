package mega.privacy.android.shared.original.core.ui.controls.snackbars

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaColors
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.composeLet


/**
 * snackbar test tag
 */
const val SNACKBAR_TEST_TAG = "mega_snackbar:snackbar_body"

/**
 * Snack bar following Mega theme
 */
@SuppressLint("MegaSnackbar")
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
    backgroundColor = MegaOriginalTheme.colors.components.toastBackground,
    contentColor = MegaOriginalTheme.colors.text.inverse,
)

@Composable
private fun SnackBarText(text: String) = Text(
    text = text,
    color = MegaOriginalTheme.colors.text.inverse,
    style = MaterialTheme.typography.subtitle2,
)

@Composable
private fun SnackbarButton(
    text: String,
    onClick: () -> Unit,
) = TextButton(
    onClick = onClick,
    colors = MegaOriginalTheme.colors.snackBarButtonColors,
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
) {
    Text(
        text = text,
        style = MaterialTheme.typography.button
    )
}

private val MegaColors.snackBarButtonColors
    @Composable
    get() = ButtonDefaults.buttonColors(
        backgroundColor = Color.Transparent,
        contentColor = text.inverseAccent,
        disabledBackgroundColor = Color.Transparent,
        disabledContentColor = text.disabled,
    )

@CombinedThemePreviews
@Composable
private fun MegaSnackbarPreview(
    @PreviewParameter(MegaSnackbarProvider::class) data: Pair<SnackbarData, Boolean>,
) {
    OriginalTempTheme(isSystemInDarkTheme()) {
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
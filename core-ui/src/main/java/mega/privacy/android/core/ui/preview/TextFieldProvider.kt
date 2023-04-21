package mega.privacy.android.core.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * TextField parameter provider for compose previews.
 */
internal class TextFieldProvider : PreviewParameterProvider<TextFieldState> {
    override val values = listOf(
        TextFieldState(),
        TextFieldState(text = "Text goes here"),
        TextFieldState(text = "Error text", error = "Error goes here")
    ).asSequence()
}

/**
 * Data class defining the possible TextField states.
 */
internal data class TextFieldState(
    val text: String = "",
    val error: String? = null,
)
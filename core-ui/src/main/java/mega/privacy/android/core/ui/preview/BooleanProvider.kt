package mega.privacy.android.core.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * Simple boolean [true, false] parameter provider for compose previews
 */
class BooleanProvider : PreviewParameterProvider<Boolean> {
    override val values = listOf(true, false).asSequence()
}
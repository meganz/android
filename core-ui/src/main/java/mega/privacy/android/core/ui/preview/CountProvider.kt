package mega.privacy.android.core.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * Simple count [1, 2] parameter provider for compose previews
 */
class CountProvider : PreviewParameterProvider<Int> {
    override val values = listOf(1, 2).asSequence()
}
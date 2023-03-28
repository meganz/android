package mega.privacy.android.core.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

internal class BooleanProvider : PreviewParameterProvider<Boolean> {
    override val values = listOf(true, false).asSequence()
}
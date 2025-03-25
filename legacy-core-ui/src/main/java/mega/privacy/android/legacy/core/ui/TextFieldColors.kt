package mega.privacy.android.legacy.core.ui

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import mega.privacy.android.shared.original.core.ui.theme.extensions.textSelectionBackground

@Composable
internal fun customTextSelectionColors() = TextSelectionColors(
    handleColor = MaterialTheme.colors.secondary,
    backgroundColor = MaterialTheme.colors.textSelectionBackground
)
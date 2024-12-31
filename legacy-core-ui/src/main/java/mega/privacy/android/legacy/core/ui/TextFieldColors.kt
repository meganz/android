package mega.privacy.android.legacy.core.ui

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
internal fun customTextSelectionColors() = TextSelectionColors(
    handleColor = MaterialTheme.colors.secondary,
    backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.4f)
)
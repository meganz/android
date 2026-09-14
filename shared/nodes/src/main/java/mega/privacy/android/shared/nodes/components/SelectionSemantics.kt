package mega.privacy.android.shared.nodes.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/**
 * Exposes the node's selection to accessibility services only while in selection mode, so that
 * ordinary browsing does not announce every row as "not selected".
 */
internal fun Modifier.selectionSemantics(
    isInSelectionMode: Boolean,
    isSelected: Boolean,
): Modifier = if (isInSelectionMode) semantics { selected = isSelected } else this

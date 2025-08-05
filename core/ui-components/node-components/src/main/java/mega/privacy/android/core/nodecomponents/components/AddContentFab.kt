package mega.privacy.android.core.nodecomponents.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import mega.android.core.ui.components.fab.MegaFab
import mega.privacy.android.icon.pack.IconPack

/**
 * FAB with plus icon with delayed visibility behavior
 * It ensures FAB is shown after completing composition to avoid flickering when used with bottom bar
 *
 * @param visible
 * @param onClick
 */
@Composable
fun AddContentFab(
    visible: Boolean,
    onClick: () -> Unit,
) {
    var readyToShow by remember { mutableStateOf(true) }

    if (visible && readyToShow) {
        MegaFab(
            onClick = onClick,
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Plus)
        )
    }

    SideEffect {
        readyToShow = visible
    }
} 
package mega.privacy.android.feature.videoeditor.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.MegaText

/**
 * Stateless video editor screen.
 *
 * Currently a placeholder; the editor UI will be implemented in a follow-up.
 */
@Composable
fun VideoEditorScreen() {
    // TODO: Implement video editor UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        MegaText("Video Editor")
    }
}

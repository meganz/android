package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Revamped Share link screen (single & multi node).
 *
 * MR0 foundation placeholder — MR1 (AND-24035) replaces this with the real
 * `MegaScaffold` + `MegaTopAppBar` (Close + gear) + node header + "Link access"
 * banner + link card + bottom-anchored "Share link" button.
 *
 * @param uiState Current [ShareLinkUiState].
 * @param onBack Invoked when the Close action is tapped.
 * @param onOpenSettings Invoked when the gear / settings action is tapped.
 * @param modifier Modifier for the root container.
 */
@Composable
fun ShareLinkScreen(
    uiState: ShareLinkUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Share link (${uiState.handles.size})")
    }
}

package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Revamped Link settings editor screen.
 *
 * MR0 foundation placeholder — MR3 (AND-24037) replaces this with the toggle rows
 * (separate key / expiry / password), inline field reveals, the bottom-anchored
 * "Save" button and the discard-changes guard.
 *
 * @param handles Node handles whose link settings are being edited.
 * @param onBack Invoked when the Close action is tapped.
 * @param modifier Modifier for the root container.
 */
@Composable
fun LinkSettingsScreen(
    handles: List<Long>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Link settings (${handles.size})")
    }
}

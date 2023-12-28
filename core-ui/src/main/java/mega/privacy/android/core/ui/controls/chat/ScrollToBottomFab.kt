package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Scroll to bottom fab
 *
 * @param modifier modifier
 * @param onClick click listener
 */
@Composable
fun ScrollToBottomFab(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FloatingActionButton(
        modifier = modifier
            .size(40.dp),
        onClick = onClick,
        shape = CircleShape,
        backgroundColor = MegaTheme.colors.icon.primary
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MegaTheme.colors.border.subtle, CircleShape)
        ) {
            Icon(
                modifier = modifier.align(Alignment.Center),
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = "Icon Arrow Down",
                tint = MegaTheme.colors.icon.inverse
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ScrollToBottomFabPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ScrollToBottomFab(onClick = {})
    }
}
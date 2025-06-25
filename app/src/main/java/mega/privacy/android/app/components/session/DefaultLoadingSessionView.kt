package mega.privacy.android.app.components.session

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Basic loading view for session containers
 */
@Composable
fun DefaultLoadingSessionView(modifier: Modifier = Modifier) =
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MegaCircularProgressIndicator()
    }

@Composable
@CombinedThemePreviews
private fun DefaultLoadingSessionViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DefaultLoadingSessionView()
    }
}
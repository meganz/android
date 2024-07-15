package mega.privacy.android.app.presentation.upload

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Upload View
 */
@Composable
fun UploadView() {
    MegaScaffold(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Upload View")
        }
    }
}

@CombinedThemePreviews
@Composable
private fun UploadViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        UploadView()
    }
}

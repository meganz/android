package mega.privacy.android.app.presentation.settings.compose.security.home.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor

@Composable
internal fun SecuritySettingsHomeView(
    onNavigateToPasscodeSettings: () -> Unit,
    onNavigateToTwoFactorSettings: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        MegaText(
            text = "Security Settings Home - Home of the QR setting",
            textColor = TextColor.Primary,
        )

    }
}
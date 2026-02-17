package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

@Composable
fun ChatExplorerScreen() {
    MegaScaffold {
        Box(modifier = Modifier.fillMaxSize().padding(it)){
            MegaText(text = "Chat Explorer Screen", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@CombinedThemePreviews
fun ChatExplorerScrenPreview() {
    AndroidThemeForPreviews {
        ChatExplorerScreen()
    }
}
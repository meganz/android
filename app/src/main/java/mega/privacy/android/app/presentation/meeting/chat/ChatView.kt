package mega.privacy.android.app.presentation.meeting.chat

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.core.ui.theme.AndroidTheme

@Composable
fun ChatView(uiState: ChatUiState = ChatUiState()) {
    Scaffold { innerPadding ->
        Text(modifier = Modifier.padding(innerPadding), text = "Hello chat fragment")
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "ChatView")
@Composable
fun ChatViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatView()
    }
}
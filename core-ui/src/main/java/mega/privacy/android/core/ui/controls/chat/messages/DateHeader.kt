package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

@Composable
fun DateHeader(
    dateString: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 16.dp)
                .align(Alignment.Center),
            text = dateString,
            style = MaterialTheme.typography.subtitle2,
            color = MegaTheme.colors.text.secondary
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewDateHeader() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DateHeader(dateString = "Friday, Nov 14, 2023")
    }
}
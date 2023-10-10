package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ImagePreviewScreen() {
    Scaffold {
        Box(modifier = Modifier.padding(it)) {
            Text(text = "Test")
        }
    }
}
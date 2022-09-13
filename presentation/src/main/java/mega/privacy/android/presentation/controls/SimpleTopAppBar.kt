package mega.privacy.android.presentation.controls

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.presentation.theme.AndroidTheme

@Composable
fun SimpleTopAppBar(titleId: Int, elevation: Boolean, onBackPressed: () -> Unit) {
    TopAppBar(
        title = {
            Text(text = stringResource(id = titleId),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium)
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White)
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkSimpleAppBarPreview")
@Composable
fun PreviewSimpleTopAppBar() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SimpleTopAppBar(titleId = 0, elevation = false, onBackPressed = {})
    }
}
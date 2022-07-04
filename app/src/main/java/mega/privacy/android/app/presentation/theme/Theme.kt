package mega.privacy.android.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.airbnb.android.showkase.annotation.ShowkaseColor
import com.airbnb.android.showkase.annotation.ShowkaseTypography
import mega.privacy.android.app.domain.entity.ThemeMode




@Composable
fun AndroidTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme: Boolean = isAppDarkTheme(mode)
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun isAppDarkTheme(mode: ThemeMode) = when (mode) {
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
    ThemeMode.System -> isSystemInDarkTheme()
}


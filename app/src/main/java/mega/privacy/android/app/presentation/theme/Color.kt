package mega.privacy.android.app.presentation.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import com.airbnb.android.showkase.annotation.ShowkaseColor

//Shared colours
//--Add any colours that are shared between the two themes here, then remove this comment--

//Light theme colours

@ShowkaseColor("Primary", "Light Theme")
val primary_600 = Color(0xFFF30C14)

@ShowkaseColor("Secondary", "Light Theme")
val secondary_300 = Color(0xFF00BFA5)

//Dark theme colours

@ShowkaseColor("Primary", "Dark Theme")
val primary_200 = Color(0xFFF46762)

@ShowkaseColor("Secondary", "Dark Theme")
val secondary_200 = Color(0xFF6DD2BF)


//Palettes

val LightColorPalette = lightColors(
    primary = primary_600,
    secondary = secondary_300,
)

val DarkColorPalette = darkColors(
    primary = primary_200,
    secondary = secondary_200,
)
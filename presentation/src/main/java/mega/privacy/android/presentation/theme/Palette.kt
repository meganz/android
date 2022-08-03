package mega.privacy.android.presentation.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import com.airbnb.android.showkase.annotation.ShowkaseColor

//Light theme colours

@ShowkaseColor("Primary", "Light Theme")
val primary_light = red_600

@ShowkaseColor("Secondary", "Light Theme")
val secondary_light = teal_300

//Dark theme colours

@ShowkaseColor("Primary", "Dark Theme")
val primary_dark = red_300

@ShowkaseColor("Secondary", "Dark Theme")
val secondary_dark = teal_200


//Palettes

val LightColorPalette = lightColors(
    primary = primary_light,
    secondary = secondary_light,
)

val DarkColorPalette = darkColors(
    primary = primary_dark,
    secondary = teal_200,
)
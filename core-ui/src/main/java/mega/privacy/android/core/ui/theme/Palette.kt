package mega.privacy.android.core.ui.theme

import android.annotation.SuppressLint
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import com.airbnb.android.showkase.annotation.ShowkaseColor

//Light theme colours

/**
 * Primary light.
 */
@ShowkaseColor("Primary", "Light Theme")
val primary_light = white

/**
 * Primary variant light.
 */
@ShowkaseColor("PrimaryVariant", "Light Theme")
val primary_variant_light = white

/**
 * Secondary light.
 */
@ShowkaseColor("Secondary", "Light Theme")
val secondary_light = teal_300

/**
 * Surface light.
 */
@ShowkaseColor("Surface", "Light Theme")
val surface_light = white

/**
 * Error light.
 */
@ShowkaseColor("Error", "Light Theme")
val error_light = red_900

/**
 * On primary light.
 */
@ShowkaseColor("OnPrimary", "Light Theme")
val on_primary_light = grey_alpha_087

/**
 * On secondary light.
 */
@ShowkaseColor("OnSecondary", "Light Theme")
val on_secondary_light = white_alpha_087

//Dark theme colours

/**
 * Primary dark.
 */
@ShowkaseColor("Primary", "Dark Theme")
val primary_dark = dark_grey

/**
 * Primary variant dark.
 */
@ShowkaseColor("PrimaryVariant", "Dark Theme")
val primary_variant_dark = dark_grey

/**
 * Secondary dark.
 */
@ShowkaseColor("Secondary", "Dark Theme")
val secondary_dark = teal_200

/**
 * Surface dark.
 */
@ShowkaseColor("Surface", "Dark Theme")
val surface_dark = dark_grey

/**
 * Error dark.
 */
@ShowkaseColor("Error", "Dark Theme")
val error_dark = red_400

/**
 * On primary dark.
 */
@ShowkaseColor("OnPrimary", "Dark Theme")
val on_primary_dark = white_alpha_087

/**
 * On secondary dark.
 */
@ShowkaseColor("OnSecondary", "Dark Theme")
val on_secondary_dark = dark_grey


//Palettes

/**
 * Light color palette.
 */
@SuppressLint("ConflictingOnColor")
val LightColorPalette = lightColors(
    primary = primary_light,
    primaryVariant = primary_variant_light,
    secondary = secondary_light,
    surface = surface_light,
    error = error_light,
    onPrimary = on_primary_light,
    onSecondary = on_secondary_light
)

/**
 * Dark color palette.
 */
@SuppressLint("ConflictingOnColor")
val DarkColorPalette = darkColors(
    primary = primary_dark,
    primaryVariant = primary_variant_dark,
    secondary = teal_200,
    surface = surface_dark,
    error = error_dark,
    onPrimary = on_primary_dark,
    onSecondary = on_secondary_dark
)
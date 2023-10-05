package mega.privacy.android.core.ui.theme

import android.annotation.SuppressLint
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors

//Light theme colours

/**
 * Primary light.
 */
val primary_light = white

/**
 * Primary variant light.
 */
val primary_variant_light = white

/**
 * Secondary light.
 */
val secondary_light = teal_300

/**
 * Surface light.
 */
val surface_light = white

/**
 * Error light.
 */
val error_light = red_900

/**
 * On primary light.
 */
val on_primary_light = grey_alpha_087

/**
 * On secondary light.
 */
val on_secondary_light = white_alpha_087

//Dark theme colours

/**
 * Primary dark.
 */
val primary_dark = dark_grey

/**
 * Primary variant dark.
 */
val primary_variant_dark = dark_grey

/**
 * Secondary dark.
 */
val secondary_dark = teal_200

/**
 * Surface dark.
 */
val surface_dark = dark_grey

/**
 * Error dark.
 */
val error_dark = red_400

/**
 * On primary dark.
 */
val on_primary_dark = white_alpha_087

/**
 * On secondary dark.
 */
val on_secondary_dark = dark_grey


//Palettes

/**
 * Light color palette.
 */
@SuppressLint("ConflictingOnColor")
val LegacyLightColorPalette = lightColors(
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
val LegacyDarkColorPalette = darkColors(
    primary = primary_dark,
    primaryVariant = primary_variant_dark,
    secondary = teal_200,
    surface = surface_dark,
    error = error_dark,
    onPrimary = on_primary_dark,
    onSecondary = on_secondary_dark
)



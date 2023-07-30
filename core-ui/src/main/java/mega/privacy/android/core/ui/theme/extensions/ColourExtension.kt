package mega.privacy.android.core.ui.theme.extensions

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color
import mega.privacy.android.core.ui.theme.amber_300
import mega.privacy.android.core.ui.theme.amber_700
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.blue_200
import mega.privacy.android.core.ui.theme.blue_300
import mega.privacy.android.core.ui.theme.blue_400
import mega.privacy.android.core.ui.theme.dark_blue_200
import mega.privacy.android.core.ui.theme.dark_blue_500
import mega.privacy.android.core.ui.theme.dark_blue_tooltip
import mega.privacy.android.core.ui.theme.dark_grey
import mega.privacy.android.core.ui.theme.green_300
import mega.privacy.android.core.ui.theme.green_400
import mega.privacy.android.core.ui.theme.green_500
import mega.privacy.android.core.ui.theme.grey_020
import mega.privacy.android.core.ui.theme.grey_050
import mega.privacy.android.core.ui.theme.grey_100
import mega.privacy.android.core.ui.theme.grey_100_alpha_060
import mega.privacy.android.core.ui.theme.grey_200
import mega.privacy.android.core.ui.theme.grey_300
import mega.privacy.android.core.ui.theme.grey_600
import mega.privacy.android.core.ui.theme.grey_700
import mega.privacy.android.core.ui.theme.grey_800
import mega.privacy.android.core.ui.theme.grey_900
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_038
import mega.privacy.android.core.ui.theme.grey_alpha_050
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.lime_green_200
import mega.privacy.android.core.ui.theme.lime_green_500
import mega.privacy.android.core.ui.theme.orange_300
import mega.privacy.android.core.ui.theme.orange_600
import mega.privacy.android.core.ui.theme.red_200
import mega.privacy.android.core.ui.theme.red_300
import mega.privacy.android.core.ui.theme.red_400
import mega.privacy.android.core.ui.theme.red_600
import mega.privacy.android.core.ui.theme.red_800
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.transparent
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_038
import mega.privacy.android.core.ui.theme.white_alpha_050
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.core.ui.theme.yellow_100
import mega.privacy.android.core.ui.theme.yellow_300
import mega.privacy.android.core.ui.theme.yellow_600
import mega.privacy.android.core.ui.theme.yellow_700
import mega.privacy.android.core.ui.theme.yellow_700_alpha_015

/**
 * Text Color Primary for Composable
 */
val Colors.textColorPrimary: Color
    get() = if (isLight) dark_grey else white

/**
 * Text Color Secondary for Composable
 */
val Colors.textColorSecondary: Color
    get() = if (isLight) grey_alpha_054 else white_alpha_054

/**
 * Color [grey_alpha_012] when light and [white_alpha_038] when dark
 */
val Colors.grey_alpha_012_white_alpha_038: Color
    get() = if (isLight) grey_alpha_012 else white_alpha_038

/**
 * Color [yellow_600] when light and [yellow_300] when dark
 */
val Colors.yellow_600_yellow_300: Color
    get() = if (isLight) yellow_600 else yellow_300

/**
 * Color [green_500] when light and [green_400] when dark
 */
val Colors.green_500_green_400: Color
    get() = if (isLight) green_500 else green_400

/**
 * Color [green_500] when light and [green_300] when dark
 */
val Colors.green_500_green_300: Color
    get() = if (isLight) green_500 else green_300


/**
 * Color [lime_green_500] when light and [lime_green_200] when dark
 */
val Colors.lime_green_500_lime_green_200: Color
    get() = if (isLight) lime_green_500 else lime_green_200

/**
 * Color [dark_blue_500] when light and [dark_blue_200] when dark
 */
val Colors.dark_blue_500_dark_blue_200: Color
    get() = if (isLight) dark_blue_500 else dark_blue_200

/**
 * Color [grey_alpha_012] when light and [white_alpha_012] when dark
 */
val Colors.grey_alpha_012_white_alpha_012: Color
    get() = if (isLight) grey_alpha_012 else white_alpha_012

/**
 * Color [blue_400] when light and [blue_200] when dark
 */
val Colors.blue_400_blue_200: Color
    get() = if (isLight) blue_400 else blue_200

/**
 * Color [white_alpha_087] when light and [grey_alpha_087] when dark
 */
val Colors.white_alpha_087_grey_alpha_087: Color
    get() = if (isLight) white_alpha_087 else grey_alpha_087

/**
 * Color [grey_alpha_087] when light and [white] when dark
 */
val Colors.grey_alpha_087_white: Color
    get() = if (isLight) grey_alpha_087 else white

/**
 * Color [red_800] when light and [red_400] when dark
 */
val Colors.red_800_red_400: Color
    get() = if (isLight) red_800 else red_400

/**
 * Color [grey_alpha_038] when light and [white_alpha_038] when dark
 */
val Colors.grey_alpha_038_white_alpha_038: Color
    get() = if (isLight) grey_alpha_038 else white_alpha_038

/**
 * Color [grey_alpha_054] when light and [white_alpha_054] when dark
 */
val Colors.grey_alpha_054_white_alpha_054: Color
    get() = if (isLight) grey_alpha_054 else white_alpha_054

/**
 * Color [red_600] when light and [red_300] when dark
 */
val Colors.red_600_red_300: Color
    get() = if (isLight) red_600 else red_300

/**
 * Color [red_600] when light and [red_400] when dark
 */
val Colors.red_600_red_400: Color
    get() = if (isLight) red_600 else red_400

/**
 * Color [grey_alpha_087] when light and [yellow_700] when dark
 */
val Colors.grey_alpha_087_yellow_700: Color
    get() = if (isLight) grey_alpha_087 else yellow_700

/**
 * Color [grey_alpha_087] when light and [white_alpha_087] when dark
 */
val Colors.grey_alpha_087_white_alpha_087: Color
    get() = if (isLight) grey_alpha_087 else white_alpha_087

/**
 * Color [amber_700] when light and [amber_300] when dark
 */
val Colors.amber_700_amber_300: Color
    get() = if (isLight) amber_700 else amber_300

/**
 * Color [grey_020] when light and [dark_grey] when dark
 */
val Colors.grey_020_dark_grey: Color
    get() = if (isLight) grey_020 else dark_grey

/**
 * Color [grey_900] when light and [grey_100] when dark
 */
val Colors.grey_900_grey_100: Color
    get() = if (isLight) grey_900 else grey_100

/**
 * Color [grey_300] when light and [grey_600] when dark
 */
val Colors.grey_300_grey_600: Color
    get() = if (isLight) grey_300 else grey_600

/**
 * Color [black] when light and [white] when dark
 */
val Colors.black_white: Color
    get() = if (isLight) black else white

/**
 * Color [white] when light and [black] when dark
 */
val Colors.white_black: Color
    get() = if (isLight) white else black

/**
 * Color [red_600] when light and [white_alpha_087] when dark
 */
val Colors.red_600_white_alpha_087: Color
    get() = if (isLight) red_600 else white_alpha_087

/**
 * Color [white] when light and [grey_700] when dark
 */
val Colors.white_grey_700: Color
    get() = if (isLight) white else grey_700

/**
 * Color [grey_200] when light and [grey_700] when dark
 */
val Colors.grey_200_grey_700: Color
    get() = if (isLight) grey_200 else grey_700

/**
 * Color [grey_020] when light and [grey_900] when dark
 */
val Colors.grey_020_grey_900: Color
    get() = if (isLight) grey_020 else grey_900

/**
 * Color when light [grey_020] and when dark [grey_700]
 */
val Colors.grey_020_grey_700: Color
    get() = if (isLight) grey_020 else grey_700

/**
 * Color when light [teal_300] and when dark [teal_200]
 */
val Colors.teal_300_teal_200: Color
    get() = if (isLight) teal_300 else teal_200


/**
 * Color when light [teal_200] and when dark [teal_300]
 * This is the opposite of [teal_300_teal_200] to be used when the background is also the opposite of the theme (snack bars for instance)
 */
val Colors.teal_200_teal_300: Color
    get() = if (isLight) teal_200 else teal_300

/**
 * Color [white] when light and [grey_alpha_087] when dark
 */
val Colors.white_grey_alpha_087: Color
    get() = if (isLight) white else grey_alpha_087

/**
 * Color when light [grey_050] and when dark [grey_800]
 */
val Colors.grey_050_grey_800: Color
    get() = if (isLight) grey_050 else grey_800

/**
 * Color when light [grey_alpha_050] and when dark [white_alpha_050]
 */
val Colors.grey_alpha_050_white_alpha_050: Color
    get() = if (isLight) grey_alpha_050 else white_alpha_050

/**
 * Color for grey when light [grey_020] and when dark [grey_800]
 */
val Colors.grey_020_grey_800: Color
    get() = if (isLight) grey_020 else grey_800

/**
 * Color when light [grey_020] and when dark [black]
 */
val Colors.grey_020_black: Color
    get() = if (isLight) grey_020 else black

/**
 * Color when light [grey_050] and when dark [grey_700]
 */
val Colors.grey_050_grey_700: Color
    get() = if (isLight) grey_050 else grey_700

/**
 * Color when light [white] and when dark [grey_800]
 */
val Colors.white_grey_800: Color
    get() = if (isLight) white else grey_800

/**
 * Color when light [grey_100_alpha_060] and when dark [grey_100]
 */
val Colors.grey_100_alpha_060_grey_100: Color
    get() = if (isLight) grey_100_alpha_060 else grey_100

/**
 * Color when light [grey_050] and when dark [grey_900]
 */
val Colors.grey_050_grey_900: Color
    get() = if (isLight) grey_050 else grey_900

/**
 * Color when light [green_400] and when dark [green_300]
 */
val Colors.green_400_green_300: Color
    get() = if (isLight) green_400 else green_300

/**
 * Color when light [orange_600] and when dark [orange_300]
 */
val Colors.orange_600_orange_300: Color
    get() = if (isLight) orange_600 else orange_300

/**
 * Color when light [red_300] and when dark [red_200]
 */
val Colors.red_300_red_200: Color
    get() = if (isLight) red_300 else red_200

/**
 * Color when light [blue_400] and when dark [blue_300]
 */
val Colors.blue_400_blue_300: Color
    get() = if (isLight) blue_400 else blue_300

/**
 * Color when light [yellow_100] and when dark [yellow_700_alpha_015]
 */
val Colors.yellow_100_yellow_700_alpha_015: Color
    get() = if (isLight) yellow_100 else yellow_700_alpha_015

/**
 * Color [black] when light and [yellow_700] when dark
 */
val Colors.black_yellow_700: Color
    get() = if (isLight) black else yellow_700

/**
 * Color when light [grey_100_alpha_060] and when dark [dark_grey]
 */
val Colors.grey_100_alpha_060_dark_grey: Color
    get() = if (isLight) grey_100_alpha_060 else dark_grey

/**
 * Color [white] when light and [transparent] when dark
 */
val Colors.white_transparent: Color
    get() = if (isLight) white else transparent

/**
 * Color [dark_blue_tooltip_white] when light and [white] when dark
 */
val Colors.dark_blue_tooltip_white: Color
    get() = if (isLight) dark_blue_tooltip else white

package mega.privacy.android.core.ui.theme.extensions

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import mega.privacy.android.core.ui.theme.dark_blue_200
import mega.privacy.android.core.ui.theme.dark_blue_500
import mega.privacy.android.core.ui.theme.dark_grey
import mega.privacy.android.core.ui.theme.green_400
import mega.privacy.android.core.ui.theme.green_500
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.lime_green_200
import mega.privacy.android.core.ui.theme.lime_green_500
import mega.privacy.android.core.ui.theme.red_300
import mega.privacy.android.core.ui.theme.red_600
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_038
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.yellow_300
import mega.privacy.android.core.ui.theme.yellow_600

/**
 * Text Color Secondary for Composable
 */
val Colors.textColorPrimary: Color
    get() = if (isLight) dark_grey else white

/**
 * Text Color Secondary for Composable
 */
@Composable
fun MaterialTheme.textColorSecondary() = if (colors.isLight) grey_alpha_054 else white_alpha_054

/**
 * Text Color for Alpha 012 Color
 */
val Colors.grey012White038: Color
    get() = if (isLight) grey_alpha_012 else white_alpha_038

/**
 * Color theme for Red 600 when light mode and 300 when dark mode
 */
val Colors.red600_300: Color
    get() = if (isLight) red_600 else red_300

/**
 * Color theme for Yellow 600 when light mode and 300 when dark mode
 */
val Colors.yellow600_300: Color
    get() = if (isLight) yellow_600 else yellow_300

/**
 * Color theme for Green 500 when light mode and 400 when dark mode
 */
val Colors.green500_400: Color
    get() = if (isLight) green_500 else green_400

/**
 * Color theme for Lime 500 when light mode and 200 when dark mode
 */
val Colors.lime500_200: Color
    get() = if (isLight) lime_green_500 else lime_green_200

/**
 * Color theme for Dark Blue 500 when light mode and 200 when dark mode
 */
val Colors.darkBlue500_200: Color
    get() = if (isLight) dark_blue_500 else dark_blue_200
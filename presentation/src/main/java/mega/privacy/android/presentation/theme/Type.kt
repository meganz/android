package mega.privacy.android.presentation.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.airbnb.android.showkase.annotation.ShowkaseTypography

/**
 * H6
 */
@ShowkaseTypography("h6", "Default Theme")
val h6 = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.15.sp
)

/**
 * Subtitle1
 */
@ShowkaseTypography("subtitle1", "Default Theme")
val subtitle1 = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.15.sp
)

/**
 * Subtitle2
 */
@ShowkaseTypography("subtitle2", "Default Theme")
val subtitle2 = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp
)

/**
 * Body1
 */
@ShowkaseTypography("body1", "Default Theme")
val body1 = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.25.sp
)

/**
 * Body2
 */
@ShowkaseTypography("body2", "Default Theme")
val body2 = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.25.sp
)

/**
 * Button
 */
@ShowkaseTypography("button", "Default Theme")
val button = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    letterSpacing = 1.50.sp
)

/**
 * Caption
 */
@ShowkaseTypography("caption", "Default Theme")
val caption = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.50.sp
)

/**
 * Typography
 */
val Typography = Typography(
    h6 = h6,
    subtitle1 = subtitle1,
    subtitle2 = subtitle2,
    body1 = body1,
    body2 = body2,
    button = button,
    caption = caption,
)
package mega.privacy.android.core.ui.theme.extensions

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

//The correct name format to create new extension function is {textStyle][Number][FontWeight],i.e:body1Medium

/**
 * Body 1 medium weight
 */
val Typography.body1Medium: TextStyle
    get() = body1.copy(fontWeight = FontWeight.Medium)

/**
 * Subtitle 2 medium weight
 */
val Typography.subtitle1medium: TextStyle
    get() = subtitle1.copy(fontWeight = FontWeight.Medium)

/**
 * Subtitle 2 medium weight
 */
val Typography.subtitle2medium: TextStyle
    get() = subtitle2.copy(fontWeight = FontWeight.Medium)

/**
 * Body 2 medium weight
 */
val Typography.body2medium: TextStyle
    get() = body2.copy(fontWeight = FontWeight.Medium)
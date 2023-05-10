package mega.privacy.android.core.ui.theme.extensions

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

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
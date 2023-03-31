package mega.privacy.android.app.presentation.changepassword.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

/**
 * Attribute to hold Test Password data
 * @param focusedColor the color of component when focused
 * @param footerMessage the message to show on the footer
 * @param footerIcon the icon to show on the footer
 */
data class TestPasswordAttribute(
    val focusedColor: Color,
    val footerMessage: String,
    @DrawableRes val footerIcon: Int
)
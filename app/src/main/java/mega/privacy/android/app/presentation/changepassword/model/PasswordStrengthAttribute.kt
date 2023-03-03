package mega.privacy.android.app.presentation.changepassword.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.changepassword.PasswordStrength

/**
 * Attributes to hold Password Strength UI Data
 * @param strength the strength of the password as [PasswordStrength]
 * @param description password strength text description
 * @param advice password strength advice for the user
 * @param color the bar color for this particular strength level
 */
data class PasswordStrengthAttribute(
    val strength: Int = PasswordStrength.INVALID.value,
    @StringRes val description: Int = R.string.pass_weak,
    @StringRes val advice: Int = R.string.passwd_weak,
    val color: Color,
)
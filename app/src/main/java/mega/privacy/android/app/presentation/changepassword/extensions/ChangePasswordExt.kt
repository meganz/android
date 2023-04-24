package mega.privacy.android.app.presentation.changepassword.extensions

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.changepassword.model.PasswordStrengthAttribute
import mega.privacy.android.core.ui.theme.extensions.dark_blue_500_dark_blue_200
import mega.privacy.android.core.ui.theme.extensions.green_500_green_400
import mega.privacy.android.core.ui.theme.extensions.lime_green_500_lime_green_200
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.yellow_600_yellow_300
import mega.privacy.android.domain.entity.changepassword.PasswordStrength

@Composable
internal fun PasswordStrength.toStrengthAttribute(): PasswordStrengthAttribute {
    return when (this) {
        PasswordStrength.VERY_WEAK -> PasswordStrengthAttribute(
            strength = PasswordStrength.VERY_WEAK.value,
            description = R.string.pass_very_weak,
            advice = R.string.passwd_weak,
            color = MaterialTheme.colors.red_600_red_300
        )
        PasswordStrength.WEAK -> PasswordStrengthAttribute(
            strength = PasswordStrength.WEAK.value,
            description = R.string.pass_weak,
            advice = R.string.passwd_weak,
            color = MaterialTheme.colors.yellow_600_yellow_300
        )
        PasswordStrength.MEDIUM -> PasswordStrengthAttribute(
            strength = PasswordStrength.MEDIUM.value,
            description = R.string.pass_medium,
            advice = R.string.passwd_medium,
            color = MaterialTheme.colors.green_500_green_400
        )
        PasswordStrength.GOOD -> PasswordStrengthAttribute(
            strength = PasswordStrength.GOOD.value,
            description = R.string.pass_good,
            advice = R.string.passwd_good,
            color = MaterialTheme.colors.lime_green_500_lime_green_200
        )
        PasswordStrength.STRONG -> PasswordStrengthAttribute(
            strength = PasswordStrength.STRONG.value,
            description = R.string.pass_strong,
            advice = R.string.passwd_strong,
            color = MaterialTheme.colors.dark_blue_500_dark_blue_200
        )
        else -> PasswordStrengthAttribute(
            strength = PasswordStrength.INVALID.value,
            color = MaterialTheme.colors.secondary
        )
    }
}
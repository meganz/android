package mega.privacy.android.app.presentation.changepassword.extensions

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.changepassword.model.PasswordStrengthAttribute
import mega.privacy.android.core.ui.theme.extensions.darkBlue500_200
import mega.privacy.android.core.ui.theme.extensions.green500_400
import mega.privacy.android.core.ui.theme.extensions.lime500_200
import mega.privacy.android.core.ui.theme.extensions.red600_300
import mega.privacy.android.core.ui.theme.extensions.yellow600_300
import mega.privacy.android.domain.entity.changepassword.PasswordStrength

@Composable
internal fun PasswordStrength.toStrengthAttribute(): PasswordStrengthAttribute {
    return when (this) {
        PasswordStrength.VERY_WEAK -> PasswordStrengthAttribute(
            strength = PasswordStrength.VERY_WEAK.value,
            description = R.string.pass_very_weak,
            advice = R.string.passwd_weak,
            color = MaterialTheme.colors.red600_300
        )
        PasswordStrength.WEAK -> PasswordStrengthAttribute(
            strength = PasswordStrength.WEAK.value,
            description = R.string.pass_weak,
            advice = R.string.passwd_weak,
            color = MaterialTheme.colors.yellow600_300
        )
        PasswordStrength.MEDIUM -> PasswordStrengthAttribute(
            strength = PasswordStrength.MEDIUM.value,
            description = R.string.pass_medium,
            advice = R.string.passwd_medium,
            color = MaterialTheme.colors.green500_400
        )
        PasswordStrength.GOOD -> PasswordStrengthAttribute(
            strength = PasswordStrength.GOOD.value,
            description = R.string.pass_good,
            advice = R.string.passwd_good,
            color = MaterialTheme.colors.lime500_200
        )
        PasswordStrength.STRONG -> PasswordStrengthAttribute(
            strength = PasswordStrength.STRONG.value,
            description = R.string.pass_strong,
            advice = R.string.passwd_strong,
            color = MaterialTheme.colors.darkBlue500_200
        )
        else -> PasswordStrengthAttribute(
            strength = PasswordStrength.INVALID.value,
            color = MaterialTheme.colors.secondary
        )
    }
}
package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.changepassword.PasswordStrength

/**
 * One render per password-strength level the screen can show, so the collapse from the SDK's five
 * grades to these three is visible rather than only asserted, plus the refused grade, which shows
 * an error in place of a level.
 *
 * The Moderate render doubles as the Weblate reference for `password_strength_moderate`.
 */
class LinkSettingsPasswordStrengthScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PasswordStrengthTooWeak() {
        // Refused, so Save is off: the label the other levels use would not say why.
        LinkSettingsWithPassword(PasswordStrength.VERY_WEAK, isSaveEnabled = false)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PasswordStrengthWeak() {
        LinkSettingsWithPassword(PasswordStrength.WEAK)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PasswordStrengthModerate() {
        LinkSettingsWithPassword(PasswordStrength.MEDIUM)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PasswordStrengthStrong() {
        LinkSettingsWithPassword(PasswordStrength.STRONG)
    }

    @Composable
    private fun LinkSettingsWithPassword(
        strength: PasswordStrength,
        isSaveEnabled: Boolean = true,
    ) {
        AndroidThemeForPreviews {
            LinkSettingsScreen(
                uiState = LinkSettingsUiState(
                    isLoading = false,
                    // A paid account, so the Pro badge does not sit over the row being shown.
                    accountType = AccountType.PRO_I,
                    isPasswordEnabled = true,
                    password = PASSWORDS.getValue(strength),
                    passwordStrength = strength,
                    isSaveEnabled = isSaveEnabled,
                ),
                onBack = {},
                onSeparateKeyEnabled = {},
                onLearnMore = {},
                onExpiryEnabled = {},
                onExpiryDateChanged = {},
                onPasswordEnabled = {},
                onPasswordChanged = {},
                onSave = {},
                onUpgrade = {},
            )
        }
    }

    private companion object {
        // Lengths that plausibly earn each grade, so the masked field looks right beside the label.
        val PASSWORDS = mapOf(
            PasswordStrength.VERY_WEAK to "abc",
            PasswordStrength.WEAK to "password12",
            PasswordStrength.MEDIUM to "passw0rd12",
            PasswordStrength.STRONG to "c0rrect-h0rse-battery-staple",
        )
    }
}

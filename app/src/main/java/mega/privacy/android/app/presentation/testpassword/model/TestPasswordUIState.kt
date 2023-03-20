package mega.privacy.android.app.presentation.testpassword.model

/**
 * UI State for Test Password Feature
 * @param isCurrentPassword ui state to handle checks whether entered password is the same as the current password
 * @param isPasswordReminderNotified ui state to handle when password reminder action has been notified
 * @param isUIEnabled ui state to check whether ui is enabled
 * @param isTestPasswordMode ui state to check whether screen is currently on test password mode layout
 */
data class TestPasswordUIState(
    val isCurrentPassword: Boolean = false,
    val isPasswordReminderNotified: Boolean = false,
    val isUIEnabled: Boolean = true,
    val isTestPasswordMode: Boolean = false
)
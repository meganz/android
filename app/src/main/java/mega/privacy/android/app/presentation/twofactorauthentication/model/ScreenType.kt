package mega.privacy.android.app.presentation.twofactorauthentication.model

/**
 * This class represents the screen flow to enable the 2FA for the [TwoFactorAuthenticationUIState]
 * @property InitialisationScreen the first screen that show explaining why is 2FA is needed
 * @property SetupScreen the second screen that contains the seeds and QR code required for the setup
 * @property VerificationScreen the third screen where the user input the verification code to enable the 2FA
 * @property VerificationPassedScreen the last screen that shows successful authentication to the user
 */
enum class ScreenType {
    InitialisationScreen,
    SetupScreen,
    VerificationScreen,
    VerificationPassedScreen
}
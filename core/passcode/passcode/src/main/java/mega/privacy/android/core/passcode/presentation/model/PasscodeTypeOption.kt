package mega.privacy.android.core.passcode.presentation.model

/**
 * The passcode formats a user can choose from when setting or changing their passcode.
 */
enum class PasscodeTypeOption {
    /**
     * A four digit pin
     */
    FourDigits,

    /**
     * A six digit pin
     */
    SixDigits,

    /**
     * A free form alphanumeric password
     */
    Alphanumeric,
}

package mega.privacy.android.app.presentation.login.model

/**
 * Enum class defining the possible errors trying to Login.
 */
enum class LoginError {
    /**
     * Empty email error.
     */
    EmptyEmail,

    /**
     * Not valid email error.
     */
    NotValidEmail,

    /**
     * Empty password error.
     */
    EmptyPassword,
}
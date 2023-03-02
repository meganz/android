package mega.privacy.android.domain.entity.changepassword

/**
 * Password strength level
 * @param value in integer based on MegaApi
 */
enum class PasswordStrength(val value: Int) {
    /**
     * Invalid password strength level
     */
    INVALID(-1),

    /**
     * Very weak password strength level
     * MegaApiJava.PASSWORD_STRENGTH_VERYWEAK
     */
    VERY_WEAK(0),

    /**
     * Weak password strength level
     * MegaApiJava.PASSWORD_STRENGTH_WEAK
     */
    WEAK(1),

    /**
     * Medium password strength level
     * MegaApiJava.PASSWORD_STRENGTH_MEDIUM
     */
    MEDIUM(2),

    /**
     * Good password strength level
     * MegaApiJava.PASSWORD_STRENGTH_GOOD
     */
    GOOD(3),

    /**
     * Strong password strength level
     * MegaApiJava.PASSWORD_STRENGTH_STRONG
     */
    STRONG(4)
}
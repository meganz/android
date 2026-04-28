package mega.privacy.android.domain.entity.login

/**
 * Result of a successful Google Sign-In.
 *
 * @property email The user's Google email address.
 * @property sub The Google unique user ID (used as MEGA password).
 * @property firstName The user's first name from Google profile, or null.
 * @property lastName The user's last name from Google profile, or null.
 */
data class GoogleSignInResult(
    val email: String,
    val sub: String,
    val firstName: String?,
    val lastName: String?,
)

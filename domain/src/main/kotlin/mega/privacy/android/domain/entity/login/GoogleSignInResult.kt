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

/**
 * The MEGA account password derived from a Google sign-in result.
 *
 * This is the single seam through which the Google-derived MEGA password is obtained.
 *
 * TODO(AND-23824): The Google `sub` is a PUBLIC identifier and must NOT
 *  remain the account password (CWE-330). Replace with a proper mechanism
 *  (random per-account password or SDK/backend SSO token) once decided.
 *  Centralised here so the swap is a single change; behaviour unchanged for now.
 */
val GoogleSignInResult.megaPassword: String get() = sub

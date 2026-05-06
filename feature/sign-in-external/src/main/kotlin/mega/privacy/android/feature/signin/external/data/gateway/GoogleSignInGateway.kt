package mega.privacy.android.feature.signin.external.data.gateway

import android.content.Context

/**
 * Gateway for Google Sign-In via Credential Manager API.
 */
interface GoogleSignInGateway {
    /**
     * Launch Google Sign-In and return the ID token JWT string.
     *
     * @param activityContext Activity context required by Credential Manager to display the sign-in UI.
     * @return The raw Google ID token (JWT).
     * @throws mega.privacy.android.domain.exception.login.GoogleSignInException on failure.
     */
    suspend fun getGoogleIdToken(activityContext: Context): String
}

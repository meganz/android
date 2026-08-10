package mega.privacy.android.feature.signin.external.data.mapper.login

import java.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mega.privacy.android.domain.entity.login.GoogleSignInResult
import javax.inject.Inject

/**
 * Maps a Google ID token (JWT) to [mega.privacy.android.domain.entity.login.GoogleSignInResult].
 *
 * Decodes the JWT payload (base64url) and extracts sub, email, given_name, family_name.
 * No signature verification — Play Services already verified the token.
 */
internal class GoogleIdTokenMapper @Inject constructor(
    private val json: Json,
) {

    @Serializable
    private data class Payload(
        val email: String? = null,
        val sub: String? = null,
        @SerialName("given_name") val givenName: String? = null,
        @SerialName("family_name") val familyName: String? = null,
    )

    /**
     * Decode the JWT and extract Google Sign-In result fields.
     *
     * @param idToken The raw Google ID token JWT string.
     * @return [mega.privacy.android.domain.entity.login.GoogleSignInResult] with extracted fields.
     * @throws IllegalArgumentException if the token is malformed or missing required fields.
     */
    operator fun invoke(idToken: String): GoogleSignInResult {
        val parts = idToken.split(".")
        require(parts.size == 3) { "Malformed JWT: expected 3 parts, got ${parts.size}" }

        val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))
        val payload = json.decodeFromString<Payload>(payloadJson)

        val email = requireNotNull(payload.email?.takeIf { it.isNotEmpty() }) {
            "Missing required field: email"
        }
        val sub = requireNotNull(payload.sub?.takeIf { it.isNotEmpty() }) {
            "Missing required field: sub"
        }

        return GoogleSignInResult(
            email = email,
            sub = sub,
            firstName = payload.givenName?.takeIf { it.isNotEmpty() },
            lastName = payload.familyName?.takeIf { it.isNotEmpty() },
        )
    }
}

package mega.privacy.android.data.extensions

/**
 * Gets the credentials if valid, false otherwise.
 */
fun String.getCredentials() =
    if (isValidCredentialsString()) {
        chunked(4)
    } else {
        null
    }

/**
 * Some credentials are only valid if the String length is 40.
 */
private fun String.isValidCredentialsString() = length == 40
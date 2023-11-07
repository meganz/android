package mega.privacy.android.domain.entity

/**
 * Entity defining all possible Api servers.
 *
 * @property value
 * @property url
 */
sealed class ApiServer(
    val value: Int,
    val url: String,
) {
    /**
     * Production server.
     */
    data object Production : ApiServer(0, "https://g.api.mega.co.nz/")

    /**
     * Staging server.
     */
    data object Staging : ApiServer(1, "https://staging.api.mega.co.nz/")

    /**
     * Staging 444 server.
     */
    data object Staging444 : ApiServer(2, "https://staging.api.mega.co.nz:444/")

    /**
     * Staging server.
     */
    data object Sandbox3 : ApiServer(3, "https://api-sandbox3.developers.mega.co.nz/")
}
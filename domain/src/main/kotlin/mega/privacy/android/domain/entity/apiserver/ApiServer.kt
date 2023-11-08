package mega.privacy.android.domain.entity.apiserver

/**
 * Entity defining all possible Api servers.
 *
 * @property value
 * @property url
 */
enum class ApiServer(
    val value: Int,
    val url: String,
) {
    /**
     * Production server.
     */
    Production(value = 0, url = "https://g.api.mega.co.nz/"),

    /**
     * Staging server.
     */
    Staging(value = 1, url = "https://staging.api.mega.co.nz/"),

    /**
     * Staging 444 server.
     */
    Staging444(value = 2, url = "https://staging.api.mega.co.nz:444/"),

    /**
     * Staging server.
     */
    Sandbox3(value = 3, url = "https://api-sandbox3.developers.mega.co.nz/"),
}
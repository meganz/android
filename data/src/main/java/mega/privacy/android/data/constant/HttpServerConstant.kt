package mega.privacy.android.data.constant

/**
 * Constants for the local HTTP proxy servers started by the MegaApi instances.
 *
 * The account API and the folder link API each start their own local HTTP proxy server,
 * and both servers can run at the same time, so their ports MUST be different.
 */
object HttpServerConstant {
    /**
     * Only accept connections from the local device (SDK default)
     */
    const val HTTP_SERVER_LOCAL_ONLY = true

    /**
     * Port for the local HTTP proxy server of the account API (SDK default port)
     */
    const val API_HTTP_SERVER_PORT = 4443

    /**
     * Port for the local HTTP proxy server of the folder link API
     */
    const val FOLDER_API_HTTP_SERVER_PORT = 4442
}

package mega.privacy.android.app.data.gateway.api

/**
 * Mega api folder gateway
 *
 * The gateway interface to the Mega Api folder functionality
 */
interface MegaApiFolderGateway {

    /**
     * Authentication token that can be used to identify the user account.
     */
    var accountAuth: String
}
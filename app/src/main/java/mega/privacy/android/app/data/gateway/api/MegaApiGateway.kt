package mega.privacy.android.app.data.gateway.api

import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface

/**
 * Mega api gateway
 *
 * The gateway interface to the Mega Api functionality
 *
 */
interface MegaApiGateway {
    /**
     * Is Multi factor auth available
     *
     * @return true if available, else false
     */
    fun multiFactorAuthAvailable(): Boolean

    /**
     * Is Multi factor auth enabled
     *
     * @param email
     * @param listener
     */
    fun multiFactorAuthEnabled(email: String?, listener: MegaRequestListenerInterface?)

    /**
     * Cancel account
     *
     * @param listener
     */
    fun cancelAccount(listener: MegaRequestListenerInterface?)

    /**
     * Registered email address for the account
     */
    val accountEmail: String?

    /**
     * Is business account
     */
    val isBusinessAccount: Boolean

    /**
     * Is master business account
     */
    val isMasterBusinessAccount: Boolean

    /**
     * Root node of the account
     *
     * All accounts have a root node, therefore if it is null the account has not been logged in or
     * initialised yet for some reason.
     *
     */
    val rootNode: MegaNode?
}
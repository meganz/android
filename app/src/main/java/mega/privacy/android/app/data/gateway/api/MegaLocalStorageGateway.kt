package mega.privacy.android.app.data.gateway.api

/**
 * MegaDBHandlerGateway gateway
 *
 * The gateway interface to the Mega DBhandler functionality
 */
interface MegaLocalStorageGateway {

    /**
     * Camera Uploads handle
     */
    val camSyncHandle: Long?
    /**
     * Media Uploads handle
     */
    val megaHandleSecondaryFolder: Long?
}
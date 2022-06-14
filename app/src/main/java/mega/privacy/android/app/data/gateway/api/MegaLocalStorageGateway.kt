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
    suspend fun getCamSyncHandle(): Long?

    /**
     * Media Uploads handle
     */
    suspend fun getMegaHandleSecondaryFolder(): Long?

    /**
     * Get cloud sort order
     * @return cloud sort order
     */
    suspend fun getCloudSortOrder(): Int

    /**
     * Get camera sort order
     * @return camera sort order
     */
    suspend fun getCameraSortOrder(): Int
}
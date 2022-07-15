package mega.privacy.android.app.data.gateway.api

import mega.privacy.android.app.data.model.UserCredentials
import mega.privacy.android.app.main.megachat.NonContactInfo

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

    /**
     * Get others sort order
     * @return others sort order
     */
    suspend fun getOthersSortOrder(): Int

    /**
     * Get user credentials
     *
     * @return user credentials or null
     */
    suspend fun getUserCredentials(): UserCredentials?


    /**
     * Get non contact by handle
     *
     * @param userHandle
     */
    suspend fun getNonContactByHandle(userHandle: Long): NonContactInfo?

    /**
     * Set non contact email
     *
     * @param userHandle
     * @param email
     */
    suspend fun setNonContactEmail(userHandle: Long, email: String)
}
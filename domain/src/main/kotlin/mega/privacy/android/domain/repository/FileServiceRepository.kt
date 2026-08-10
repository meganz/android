package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions

/**
 * Repository for file service(cache) operations.
 */
interface FileServiceRepository {

    /**
     * Get the current reclaim options for the logged-in user.
     *
     * @return the current [FileServiceReclaimOptions], or null if unavailable.
     */
    suspend fun getReclaimOptions(): FileServiceReclaimOptions?

    /**
     * Set the reclaim options for the logged-in user's file service.
     *
     * @param options the [FileServiceReclaimOptions] to set.
     */
    suspend fun setReclaimOptions(options: FileServiceReclaimOptions)

    /**
     * Trigger a reclaim operation for the logged-in user.
     *
     * @param options Options for this reclaim run. If null, the currently configured options
     * are used (see [setReclaimOptions]).
     * @return the number of bytes reclaimed.
     */
    suspend fun reclaim(options: FileServiceReclaimOptions? = null): Long

    /**
     * Set the reclaim options for the public link file service.
     *
     * @param options the [FileServiceReclaimOptions] to set.
     */
    suspend fun setPublicLinkReclaimOptions(options: FileServiceReclaimOptions)
}

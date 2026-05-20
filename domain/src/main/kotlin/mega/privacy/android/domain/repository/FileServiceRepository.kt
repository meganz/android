package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions

/**
 * Repository for file service operations.
 */
interface FileServiceRepository {

    /**
     * Get the current reclaim options.
     *
     * @return the current [FileServiceReclaimOptions], or null if unavailable.
     */
    suspend fun getReclaimOptions(): FileServiceReclaimOptions?

    /**
     * Set the reclaim options.
     *
     * @param options the [FileServiceReclaimOptions] to set.
     */
    suspend fun setReclaimOptions(options: FileServiceReclaimOptions)

    /**
     * Trigger a reclaim operation.
     *
     * @param options Options for this reclaim run. If null, the currently configured options
     * are used (see [setReclaimOptions]).
     * @return the number of bytes reclaimed.
     */
    suspend fun reclaim(options: FileServiceReclaimOptions? = null): Long
}

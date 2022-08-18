package mega.privacy.android.domain.usecase

/**
 * Clear sync records if necessary
 *
 */
interface ClearSyncRecords {

    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke()
}

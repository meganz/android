package mega.privacy.android.domain.usecase

/**
 * Set Primary sync handle
 *
 */
interface SetPrimarySyncHandle {
    /**
     * Invoke
     *
     * @param newPrimaryHandle
     * @return
     */
    suspend operator fun invoke(newPrimaryHandle: Long)
}

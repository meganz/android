package mega.privacy.android.domain.usecase

/**
 * Set Secondary sync handle
 *
 */
interface SetSecondarySyncHandle {
    /**
     * Invoke
     *
     * @param newSecondaryHandle
     * @return
     */
    suspend operator fun invoke(newSecondaryHandle: Long)
}

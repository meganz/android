package mega.privacy.android.app.domain.usecase

/**
 * Set secondary camera upload sync handle
 */
fun interface SetSecondarySyncHandle {
    /**
     * Set Camera Uploads Secondary handle
     *
     * @param secondaryHandle
     * @return
     */
    suspend operator fun invoke(secondaryHandle: Long)
}

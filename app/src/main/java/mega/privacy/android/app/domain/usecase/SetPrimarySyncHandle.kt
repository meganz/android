package mega.privacy.android.app.domain.usecase

/**
 * Set primary camera upload sync handle
 */
fun interface SetPrimarySyncHandle {
    /**
     * Set Camera Uploads Primary handle
     *
     * @param primaryHandle
     * @return
     */
    suspend operator fun invoke(primaryHandle: Long)
}

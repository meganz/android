package mega.privacy.android.domain.usecase

/**
 * Delete camera upload sync record
 *
 */
fun interface DeleteSyncRecord {

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(path: String, isSecondary: Boolean)
}
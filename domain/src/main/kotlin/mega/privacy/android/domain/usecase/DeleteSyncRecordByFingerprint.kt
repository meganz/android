package mega.privacy.android.domain.usecase

/**
 * Delete camera upload sync record by fingerprint
 *
 */
fun interface DeleteSyncRecordByFingerprint {

    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(originalPrint: String, newPrint: String, isSecondary: Boolean)
}

package mega.privacy.android.app.domain.usecase

/**
 * Delete camera upload sync record by fingerprint
 *
 */
interface DeleteSyncRecordByFingerprint {

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(originalPrint: String, newPrint: String, isSecondary: Boolean)
}

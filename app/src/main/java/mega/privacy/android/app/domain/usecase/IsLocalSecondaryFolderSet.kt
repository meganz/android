package mega.privacy.android.app.domain.usecase

/**
 * Check the availability of camera upload local secondary folder
 *
 * If it's a path in internal storage, check its existence
 * If it's a path in SD card, check the corresponding DocumentFile's existence
 *
 * @return true, if secondary folder is available
 */
interface IsLocalSecondaryFolderSet {

    /**
     * Invoke
     *
     * @return whether local secondary folder is set
     */
    suspend operator fun invoke(): Boolean
}

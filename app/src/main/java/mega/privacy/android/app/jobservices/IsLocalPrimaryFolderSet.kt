package mega.privacy.android.app.jobservices

/**
 * Check the availability of camera upload local primary folder
 *
 * If it's a path in internal storage, check its existence
 * If it's a path in SD card, check the corresponding DocumentFile's existence
 *
 * @return true, if local primary folder is available
 */
interface IsLocalPrimaryFolderSet {

    /**
     * Invoke
     *
     * @return whether local primary folder is set
     */
    operator fun invoke(): Boolean
}

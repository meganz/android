package mega.privacy.android.app.jobservices

/**
 * Check preferences if secondary media folder is enabled
 *
 * @return true, if secondary enabled
 */
interface IsSecondaryFolderEnabled {

    /**
     * Invoke
     *
     * @return if secondary is enabled
     */
    operator fun invoke(): Boolean
}

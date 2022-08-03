package mega.privacy.android.domain.usecase

/**
 * Check preferences if secondary media folder is enabled
 *
 * @return true, if secondary enabled
 */
fun interface IsSecondaryFolderEnabled {

    /**
     * Invoke
     *
     * @return if secondary is enabled
     */
    operator fun invoke(): Boolean
}

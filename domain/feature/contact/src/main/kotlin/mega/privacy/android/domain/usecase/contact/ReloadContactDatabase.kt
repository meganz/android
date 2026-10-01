package mega.privacy.android.domain.usecase.contact

/**
 * Reload contact database
 * It will recreate contact database if the database invalid or force
 */
fun interface ReloadContactDatabase {
    /**
     * Invoke
     *
     * @param isForceReload
     */
    suspend operator fun invoke(isForceReload: Boolean)
}
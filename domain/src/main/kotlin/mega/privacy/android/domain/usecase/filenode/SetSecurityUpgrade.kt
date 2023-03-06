package mega.privacy.android.domain.usecase.filenode

/**
 * Set account security upgrade
 *
 */
fun interface SetSecurityUpgrade {
    /**
     * Invoke
     *
     * @param isSecurityUpgrade
     */
    suspend operator fun invoke(isSecurityUpgrade: Boolean)
}

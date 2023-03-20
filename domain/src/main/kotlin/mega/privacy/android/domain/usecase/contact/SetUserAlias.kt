package mega.privacy.android.domain.usecase.contact

/**
 * Set user alias name
 */
fun interface SetUserAlias {

    /**
     * invoke
     *
     * @param name updated nick name
     * @param userHandle user handle
     * @return [String] updated nick name
     */
    suspend operator fun invoke(name: String?, userHandle: Long): String?
}
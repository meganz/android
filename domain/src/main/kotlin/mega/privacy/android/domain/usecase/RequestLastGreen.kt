package mega.privacy.android.domain.usecase

/**
 * Request last green
 */
fun interface RequestLastGreen {

    /**
     * Invoke.
     *
     * @param userHandle User handle.
     */
    suspend operator fun invoke(userHandle: Long)
}
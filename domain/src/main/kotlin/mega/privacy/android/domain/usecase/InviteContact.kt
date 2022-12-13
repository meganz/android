package mega.privacy.android.domain.usecase

/**
 * Use case for invite contact
 */
fun interface InviteContact {

    /**
     * Invoke.
     *
     * @param email    User email
     * @return         Result
     */
    suspend operator fun invoke(
        email: String,
    ): Boolean
}
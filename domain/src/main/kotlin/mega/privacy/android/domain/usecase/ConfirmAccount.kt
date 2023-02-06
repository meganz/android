package mega.privacy.android.domain.usecase

/**
 * Use case for confirming a new account.
 */
fun interface ConfirmAccount {

    /**
     * Invoke.
     *
     * @param confirmationLink Confirmation link of the new account.
     * @param password         Password of the new account.
     */
    suspend operator fun invoke(confirmationLink: String, password: String)
}
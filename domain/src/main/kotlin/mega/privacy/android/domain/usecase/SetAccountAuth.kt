package mega.privacy.android.domain.usecase

/**
 * Use case to set the Authentication Token used to identify the user account
 */
fun interface SetAccountAuth {

    /**
     * Sets the Authentication Token used to identify the User Account
     */
    suspend operator fun invoke()
}
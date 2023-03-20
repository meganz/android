package mega.privacy.android.domain.usecase.account

/**
 * Interface to set secure flag value to true or false
 */
fun interface SetSecureFlag {

    /**
     * Invoke
     *
     * @param enable : Boolean value
     */
    suspend operator fun invoke(enable: Boolean)
}

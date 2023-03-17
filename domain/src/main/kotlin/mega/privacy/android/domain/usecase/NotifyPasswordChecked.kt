package mega.privacy.android.domain.usecase

/**
 * Use Case to Notify the user has successfully checked his password
 */
fun interface NotifyPasswordChecked {
    /**
     * Invoke the Use Case
     */
    suspend operator fun invoke()
}
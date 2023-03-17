package mega.privacy.android.domain.usecase

/**
 * Use Case to notify the user wants to totally disable the password check
 */
fun interface BlockPasswordReminder {
    /**
     * Invoke the Use Case
     */
    suspend operator fun invoke()
}
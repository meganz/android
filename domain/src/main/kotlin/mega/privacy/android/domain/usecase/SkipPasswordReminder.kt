package mega.privacy.android.domain.usecase


/**
 * Use Case to notify the user has successfully skipped the password check
 */
fun interface SkipPasswordReminder {
    /**
     * Invoke the Use case
     */
    suspend operator fun invoke()
}
package mega.privacy.android.app.domain.usecase

/**
 * Use case for getting the number of unread user alerts for the logged in user.
 */
fun interface GetNumUnreadUserAlerts {

    /**
     * Invoke.
     *
     * @return Number of unread user alerts.
     */
    suspend operator fun invoke(): Int
}
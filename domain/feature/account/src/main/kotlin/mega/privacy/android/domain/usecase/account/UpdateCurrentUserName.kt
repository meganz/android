package mega.privacy.android.domain.usecase.account

/**
 * Update current user name
 *
 */
fun interface UpdateCurrentUserName {
    /**
     * Invoke
     *
     * @param oldFirstName
     * @param oldLastName
     * @param newFirstName
     * @param newLastName
     */
    suspend operator fun invoke(
        oldFirstName: String,
        oldLastName: String,
        newFirstName: String,
        newLastName: String,
    )
}
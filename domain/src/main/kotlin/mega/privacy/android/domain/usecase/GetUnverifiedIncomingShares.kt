package mega.privacy.android.domain.usecase

/**
 * GetUnverifiedIncomingShares Use case
 */
fun interface GetUnverifiedIncomingShares {

    /**
     * @return Flow of unverified incoming shares
     */
    suspend operator fun invoke(): Int
}
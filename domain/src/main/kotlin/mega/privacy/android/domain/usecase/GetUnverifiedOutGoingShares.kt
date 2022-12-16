package mega.privacy.android.domain.usecase

/**
 * GetUnverifiedOutGoingShares Use case
 */
fun interface GetUnverifiedOutGoingShares {

    /**
     * @return Flow of unverified outgoing shares
     */
    suspend operator fun invoke(): Int
}
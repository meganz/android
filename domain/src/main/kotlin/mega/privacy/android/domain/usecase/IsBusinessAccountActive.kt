package mega.privacy.android.domain.usecase

/**
 * Use case that checks whether the Business Account is Active or not
 */
fun interface IsBusinessAccountActive {

    /**
     * Calls the Use Case to check whether the Business Account is Active or not
     */
    suspend operator fun invoke(): Boolean
}
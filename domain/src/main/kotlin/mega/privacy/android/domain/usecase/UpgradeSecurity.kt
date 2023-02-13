package mega.privacy.android.domain.usecase

/**
 * Upgrade security use case
 */
fun interface UpgradeSecurity {

    /**
     * Invoke
     */
    suspend operator fun invoke()
}
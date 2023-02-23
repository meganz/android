package mega.privacy.android.domain.usecase

/**
 * Use case to check if the master key has been exported or not
 */
fun interface IsMasterKeyExported {

    /**
     * Invoke
     * @return if the master key has been exported or not as [Boolean]
     */
    suspend operator fun invoke(): Boolean
}
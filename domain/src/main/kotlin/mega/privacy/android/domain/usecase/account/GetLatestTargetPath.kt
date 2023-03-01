package mega.privacy.android.domain.usecase.account

/**
 * Get latest target path of move/copy
 */
fun interface GetLatestTargetPath {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Long?
}
package mega.privacy.android.domain.usecase

/**
 * Not enough quota to proceed with camera upload
 *
 */
fun interface IsNotEnoughQuota {
    /**
     * Invoke
     *
     * @return true if not enough quota, else false
     */
    suspend operator fun invoke(): Boolean
}

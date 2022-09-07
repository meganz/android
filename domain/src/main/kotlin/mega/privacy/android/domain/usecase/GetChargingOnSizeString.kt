package mega.privacy.android.domain.usecase

/**
 * Get charging on size
 *
 */
fun interface GetChargingOnSizeString {

    /**
     * Invoke
     *
     * @return charging on size string
     */
    suspend operator fun invoke(): String
}

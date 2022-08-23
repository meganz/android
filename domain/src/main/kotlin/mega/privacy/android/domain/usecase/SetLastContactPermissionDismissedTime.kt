package mega.privacy.android.domain.usecase

/**
 * Set last contact permission dismissed time
 */
fun interface SetLastContactPermissionDismissedTime {
    /**
     * invoke
     *
     * @param time
     */
    suspend operator fun invoke(time: Long)
}
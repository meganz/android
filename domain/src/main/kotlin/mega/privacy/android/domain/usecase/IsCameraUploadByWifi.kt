package mega.privacy.android.domain.usecase

/**
 * Use case to check if camera upload sync is by wifi only
 */
fun interface IsCameraUploadByWifi {

    /**
     * Invoke
     *
     * @return sync is by wifi
     */
    suspend operator fun invoke(): Boolean
}

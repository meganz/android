package mega.privacy.android.app.domain.usecase

/**
 * Use case to check if camera upload sync is by wifi only
 */
fun interface IsCameraUploadByWifi {

    /**
     * Invoke
     *
     * @return sync is by wifi
     */
    operator fun invoke(): Boolean
}

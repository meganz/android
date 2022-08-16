package mega.privacy.android.app.domain.usecase

/**
 * Check if camera upload is by wifi only and if wifi is disabled
 *
 * @return true, if sync by wifi only and wifi disabled (camera upload cannot execute)
 */
interface IsWifiNotSatisfied {

    /**
     * Invoke
     *
     * @return if camera upload can execute
     */
    suspend operator fun invoke(): Boolean
}

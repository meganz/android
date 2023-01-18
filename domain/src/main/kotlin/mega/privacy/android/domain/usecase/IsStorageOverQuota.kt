package mega.privacy.android.domain.usecase

/**
 * Is storage over quota
 *
 * @constructor Create empty Is storage over quota
 */
fun interface IsStorageOverQuota {
    /**
     * Invoke
     *
     * @return true if over quota, else false
     */
    suspend operator fun invoke(): Boolean
}
package mega.privacy.android.domain.usecase

/**
 * Push multi factor auth change
 *
 * @constructor Create empty Push multi factor auth change
 */
fun interface PushMultiFactorAuthChange {
    /**
     * Invoke
     *
     * @param isEnable
     */
    suspend operator fun invoke(isEnable: Boolean)
}
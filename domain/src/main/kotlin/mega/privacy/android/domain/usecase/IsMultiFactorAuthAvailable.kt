package mega.privacy.android.domain.usecase

/**
 * Is multi factor auth available
 *
 */
fun interface IsMultiFactorAuthAvailable {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Boolean
}
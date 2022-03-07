package mega.privacy.android.app.domain.usecase

/**
 * Is multi factor auth available
 *
 */
interface IsMultiFactorAuthAvailable {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Boolean
}
package mega.privacy.android.app.domain.usecase

/**
 * Refresh passcode lock preference
 *
 */
interface RefreshPasscodeLockPreference {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Boolean
}
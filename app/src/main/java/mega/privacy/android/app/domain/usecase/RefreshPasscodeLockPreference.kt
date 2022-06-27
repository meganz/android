package mega.privacy.android.app.domain.usecase

/**
 * Refresh passcode lock preference
 *
 */
fun interface RefreshPasscodeLockPreference {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Boolean
}
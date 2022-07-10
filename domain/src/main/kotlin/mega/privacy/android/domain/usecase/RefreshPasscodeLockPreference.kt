package mega.privacy.android.domain.usecase

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
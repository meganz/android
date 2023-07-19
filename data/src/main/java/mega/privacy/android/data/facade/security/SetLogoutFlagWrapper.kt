package mega.privacy.android.data.facade.security

/**
 * Set logout flag wrapper
 */
interface SetLogoutFlagWrapper {
    /**
     * Set logout flag
     *
     * @param isLoggingOut
     */
    operator fun invoke(isLoggingOut: Boolean)
}
package mega.privacy.android.app.utils.wrapper

import mega.privacy.android.app.MegaApplication

/**
 * Set logout flag wrapper
 */
interface SetLogoutFlagWrapper {
    /**
     * Set logout flag
     *
     * @param isLoggingOut
     */
    operator fun invoke(isLoggingOut: Boolean) {
        MegaApplication.isLoggingOut = isLoggingOut
    }
}